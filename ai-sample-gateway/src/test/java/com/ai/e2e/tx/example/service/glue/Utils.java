package com.ai.e2e.tx.example.service.glue;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public final class Utils {

  @SneakyThrows
  public static <K> K retryFor(
          final int numberOfRetries, final int waitForMilliseconds, final Supplier<K> functionToExecute
  ) {
    int i = 0;
    Throwable lastThrowable = null;

    while (i < numberOfRetries) {
      try {
        return functionToExecute.get();
      } catch (final Throwable throwable) {
        lastThrowable = throwable;
        i++;
        log.warn(buildWarningMessage(numberOfRetries, i), throwable);
        Thread.sleep(waitForMilliseconds);
      }
    }

    if (lastThrowable != null) {
      throw lastThrowable;
    } else {
      throw new IllegalStateException("Supplier did not return an exception");
    }
  }

  private static String buildWarningMessage(final int numberOfRetries, final int i) {
    return String.format("Failed to execute given supplier. " + (i < numberOfRetries
            ? "Retrying %d time."
            : "Failing after %d times."), i);
  }

}
