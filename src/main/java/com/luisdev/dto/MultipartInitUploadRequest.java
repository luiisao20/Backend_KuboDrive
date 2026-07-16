package com.luisdev.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MultipartInitUploadRequest extends FileInitUploadRequest {
}
