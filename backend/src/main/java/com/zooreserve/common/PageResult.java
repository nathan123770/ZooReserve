package com.zooreserve.common;

import java.util.List;

public record PageResult<T>(long total, int page, int size, List<T> records) {

  public static <T> PageResult<T> firstPage(List<T> records) {
    return new PageResult<>(records.size(), 1, records.size(), records);
  }
}
