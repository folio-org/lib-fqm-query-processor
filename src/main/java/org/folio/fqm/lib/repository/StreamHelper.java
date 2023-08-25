package org.folio.fqm.lib.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamHelper {

  private StreamHelper() {
    throw new IllegalStateException("Utility class");
  }

  static <T> Stream<List<T>> chunk(Stream<T> stream, int size) {
    if (size <= 1) {
      throw new IllegalArgumentException("Invalid size parameter: " + size);
    }

    Iterator<T> iterator = stream.iterator();
    Iterator<List<T>> listIterator = new Iterator<>() {
      public boolean hasNext() {
        return iterator.hasNext();
      }

      public List<T> next() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size && iterator.hasNext(); i++) {
          result.add(iterator.next());
        }
        return result;
      }
    };
    return StreamSupport.stream(((Iterable<List<T>>) () -> listIterator).spliterator(), false)
      .onClose(stream::close);
  }
}
