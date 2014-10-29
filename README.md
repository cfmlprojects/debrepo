## debrepo

This project creates debian (apt) repositories for .deb files.

Theoretically does the same-ish stuff dpkg-scanpackages does, but without the need to be on Debian,
or having dpkg installed.  As this is pure java, all you need is a JRE.

There is an ant task (debrepo.ant.DebRepoTask) for easy utilization in builds.

Ant example:
```
<debrepo debsDir="${dist.dir}" repoDir="${deb.repo}" verbose="true" 
 key="9266D40B" passphrase="testtest" keyring="/path/to/a/secring.gpg"
 label="Debrepo" description="Debrepo Repo"/>
```

That would copy the debs from "debsDir" into "repoDir" and then generate the repository metadata. (You can
specify the repoDir alone if you already copied the debs there.)

It would also sign the Release, creating a Release.gpg in the repo.  Signing is optional, but recommended. 

Test the repository on a debian system using something like:

sudo sh -c 'echo "deb http://192.168.0.100:8088/debs /" >> /etc/apt/sources.list.d/debrepo.list'
sudo apt-get update
sudo apt-get install yourAwesomelyPackagedPackageName

This assumes the repoDir attribute above is the directory that http://.../debs resolves to ("debs" here).

This tool is meant to be used with jdeb, or ant-deb-task, and actually has jdeb shaded in, so no other dependencies
are required.  (I include jdeb because it has signing capabilities while ant-deb-task does not, and since it already
has all the needed dependencies shaded into it, well, two birds, as it were.)