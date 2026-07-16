package com.luisdev.service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import io.minio.messages.Part;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

  private final MinioClient minioClient;
  private final MinioAsyncClient minioAsyncClient;
  private final String bucketName;

  public MinioService(
      @Value("${minio.url}") String url,
      @Value("${minio.access-key}") String accessKey,
      @Value("${minio.secret-key}") String secretKey,
      @Value("${minio.bucket-name}") String bucketName) {

    this.minioClient = MinioClient.builder()
        .endpoint(url)
        .credentials(accessKey, secretKey)
        .build();
    this.minioAsyncClient = MinioAsyncClient.builder()
        .endpoint(url)
        .credentials(accessKey, secretKey)
        .build();
    this.bucketName = bucketName;
  }

  @PostConstruct
  public void initBucket() {
    try {
      boolean found = minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(bucketName).build());

      if (!found) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(bucketName).build());
        System.out.println("Bucket de MinIO '" + bucketName + "' creado exitosamente.");
      }
    } catch (Exception e) {
      throw new RuntimeException("Error al verificar o crear el bucket de MinIO", e);
    }
  }

  public String generatePresignedUploadUrl(String minioObjectId) {
    try {
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucketName)
              .object(minioObjectId)
              .expiry(10, TimeUnit.MINUTES)
              .build());
    } catch (Exception e) {
      throw new RuntimeException("Error generating presigned upload url", e);
    }
  }

  public String generatePresignedDownloadUrl(String minioObjectId, String originalName) {
    try {
      Map<String, String> reqParams = new HashMap<>();
      reqParams.put("response-content-disposition", "attachment; filename=\"" + originalName + "\"");

      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucketName)
              .object(minioObjectId)
              .expiry(10, TimeUnit.MINUTES)
              .extraQueryParams(reqParams)
              .build());
    } catch (Exception e) {
      throw new RuntimeException("Error generating presigned download url", e);
    }
  }

  public void deleteFileFromMinio(String minioObjectId) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucketName)
              .object(minioObjectId)
              .build());
    } catch (Exception e) {
      throw new RuntimeException("Error al eliminar el archivo físico en MinIO", e);
    }
  }

  public String createMultipartUpload(String minioObjectId, String contentType) {
    try {
      Multimap<String, String> headers = HashMultimap.create();
      headers.put("Content-Type", contentType != null ? contentType : "application/octet-stream");

      return minioAsyncClient.createMultipartUploadAsync(
          bucketName,
          null,
          minioObjectId,
          headers,
          HashMultimap.create()
      ).join().result().uploadId();
    } catch (Exception e) {
      throw new RuntimeException("Error al crear la subida multipart en MinIO", e);
    }
  }

  public List<String> generatePresignedPartUploadUrls(String minioObjectId, String uploadId, int totalParts,
      int expiryMinutes) {
    try {
      List<String> urls = new ArrayList<>(totalParts);
      for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
        urls.add(generatePresignedPartUploadUrl(minioObjectId, uploadId, partNumber, expiryMinutes));
      }
      return urls;
    } catch (Exception e) {
      throw new RuntimeException("Error generando URLs prefirmadas para las partes", e);
    }
  }

  public String generatePresignedPartUploadUrl(String minioObjectId, String uploadId, int partNumber,
      int expiryMinutes) {
    try {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("uploadId", uploadId);
      queryParams.put("partNumber", String.valueOf(partNumber));

      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucketName)
              .object(minioObjectId)
              .expiry(expiryMinutes, TimeUnit.MINUTES)
              .extraQueryParams(queryParams)
              .build());
    } catch (Exception e) {
      throw new RuntimeException("Error generando URL prefirmada para la parte " + partNumber, e);
    }
  }

  public void completeMultipartUpload(String minioObjectId, String uploadId, List<Part> parts) {
    try {
      Part[] partsArray = parts.stream()
          .sorted((a, b) -> Integer.compare(a.partNumber(), b.partNumber()))
          .toArray(Part[]::new);

      minioAsyncClient.completeMultipartUploadAsync(
          bucketName,
          null,
          minioObjectId,
          uploadId,
          partsArray,
          HashMultimap.create(),
          HashMultimap.create()
      ).join();
    } catch (Exception e) {
      throw new RuntimeException("Error al completar la subida multipart en MinIO", e);
    }
  }

  public void abortMultipartUpload(String minioObjectId, String uploadId) {
    try {
      minioAsyncClient.abortMultipartUploadAsync(
          bucketName,
          null,
          minioObjectId,
          uploadId,
          HashMultimap.create(),
          HashMultimap.create()
      ).join();
    } catch (Exception e) {
      throw new RuntimeException("Error al abortar la subida multipart en MinIO", e);
    }
  }
}
