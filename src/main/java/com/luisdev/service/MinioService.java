package com.luisdev.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

  private final MinioClient minioClient;
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
}
