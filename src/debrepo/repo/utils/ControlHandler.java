package debrepo.repo.utils;

import debrepo.repo.packages.PackageEntry;

public class ControlHandler {
  private String controlContent;

  public void setControlContent(String controlContent) {
    this.controlContent = controlContent.trim();
  }

  public boolean hasControlContent() {
    return controlContent != null ? true : false;
  }

  private void parseControl(PackageEntry packageEntry) throws RuntimeException {
    if (controlContent == null) {
      throw new RuntimeException("no controlContent to parse");
    }
    String[] lines = controlContent.split("\\r?\\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String[] stmt = line.split(":", 2);
      if (stmt.length != 2) {
        continue;
      }
      String key = stmt[0].trim();
      String value = stmt[1].trim();
      if (key.equals("Package")) {
        packageEntry.setPackageName(value);
      } else if (key.equals("Version")) {
        packageEntry.setVersion(value);
      } else if (key.equals("Architecture")) {
        packageEntry.setArchitecture(value);
      } else if (key.equals("Maintainer")) {
        packageEntry.setMaintainer(value);
      } else if (key.equals("Installed-Size")) {
        packageEntry.setInstalled_size(value);
      } else if (key.equals("Depends")) {
        packageEntry.setDepends(value);
      } else if (key.equals("Section")) {
        packageEntry.setSection(value);
      } else if (key.equals("Priority")) {
        packageEntry.setPriority(value);
      } else if (key.equals("Description")) {
        packageEntry.setDescription(value);
      }
    }
  }

  public void handle(PackageEntry packageEntry) throws RuntimeException {
    parseControl(packageEntry);
  }

}
