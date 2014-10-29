package debrepo.repo.packages;

import java.util.ArrayList;

public class Packages {
  private ArrayList<PackageEntry> packages = new ArrayList<PackageEntry>();

  public void addPackageEntry(PackageEntry packageEntry) {
    packages.add(packageEntry);
  }

  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    for (PackageEntry p : packages) {
      stringBuffer.append(p.toString());
      stringBuffer.append("\n");
    }
    return stringBuffer.toString();
  }

}
