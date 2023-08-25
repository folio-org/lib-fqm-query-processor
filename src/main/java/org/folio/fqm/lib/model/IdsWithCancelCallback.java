package org.folio.fqm.lib.model;

import java.util.List;
import java.util.UUID;

public record IdsWithCancelCallback(List<UUID> ids, Runnable cancelFunction) {

  public void cancel() {
    cancelFunction.run();
  }
}
