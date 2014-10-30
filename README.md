## debrepo

This project creates debian (apt) repositories for .deb files.

Theoretically does the same-ish stuff dpkg-scanpackages does, but without the need to be on Debian,
or having dpkg installed.  As this is pure java, all you need is a JRE.

There is an ant task (debrepo.ant.DebRepoTask) for easy utilization in builds.

## Ant example

~/creds/secret.properties
```
debrepo.sign.key.passphrase=inconceivable
debrepo.sign.key.ide=D0B067EC
debrepo.sign.keyring=${user.home}/creds/debrepo-secring.gpg
```

./project/build.xml
```
<loadproperties srcFile="${user.home}/creds/secret.properties"/>
<taskdef name="debrepo" classname="debrepo.ant.DebRepoTask" classpath="debrepo.jar"/>
<debrepo label="Debrepo" description="Debrepo Repo" verbose="false"
 debsDir="${dist.dir}" repoDir="${deb.repo}" 
 key="${debrepo.sign.key.id}" passphrase="${debrepo.sign.key.passphrase}"
 keyring="${debrepo.sign.keyring}" />
```

That would copy the debs from "debsDir" into "repoDir" and then generate the repository metadata. (You can
specify the repoDir alone if you already copied the debs there.)

It would also sign the Release, creating a Release.gpg in the repo.  Signing is optional, but recommended. 


Test the repository on a debian system using something like:
```
sudo sh -c 'echo "deb http://192.168.0.100:8088/debs /" >> /etc/apt/sources.list.d/debrepo.list'
```
Which will add the repository to apt.  This assumes the repoDir attribute above is the directory that 
http://.../debs resolves to ("debs" here). 


and then optionally import the signing key:
```
gpg --keyserver keys.gnupg.net --recv-key yourSignerKeyId
gpg -a --export yourSignerKeyId | sudo apt-key add -
```
The signing key would be generared with gpg --gen-key, and then sent to a keyserver, or you could manually
add the keys.  FWIW, there's an example deb in [./tests](./tests/debrepo-keyring) demonstrating how you
can update keyrings.  See [./build/build.xml](./build/build.xml) for an example of this thing bundling
itself up, all signed y todo.


Anyways, then you can test installing your package from your repo:
```
sudo apt-get update
sudo apt-get install yourAwesomelyPackagedPackageName
```


As [jdeb](https://github.com/tcurdt/jdeb) has all the needed classes shadded in, it is included in the 
debrepo jar, and as I really like the way [ant-deb-task](https://code.google.com/p/ant-deb-task/) works,
there's also an ant task (debrepo.ant.DebTask) which wraps the jdeb task with the ant-deb-task signature:

```
<loadproperties srcFile="${user.home}/creds/secret.properties"/>
<taskdef name="deb" classname="debrepo.ant.DebTask" classpath="debrepo.jar"/>
<deb
    todir="${deb.dir}"
    package="railo-application"
    section="web"
    depends="java-common"
    key="${debrepo.sign.key.id}" passphrase="${debrepo.sign.key.passphrase}"
    keyring="${debrepo.sign.keyring}">
    <version upstream="${debrepo.version}"/>
    <maintainer name="Packager Name" email="packager@email.addy"/>
    <description synopsis="This is the summary">This is the long description</description>
	<tarfileset file="${debrepo.jar.file}" prefix="usr/local/bin" filemode="755"/>
</deb>
```

## building

This uses the [cfdistro](https://github.com/cfmlprojects/cfdistro) project, which adds a bunch of ant
tasks and stuff.  If you're feeling lucky, just type:
```
./debrepo build.mvn
```

To build and add the resulting artifact to your local cfdistro repo.  If you run into any problems you
can convert build/build.xml to a normal ant build pretty easy-- it's nothing too special beyond the thing
that defines a taskdef using a dependency (which *is* pretty tits), I reckon.

If you import it into eclipse run the build once and it'll fetch the dependencies, resolving the classpath.


So that about sums it up.  Theoretically the debrepo jar is all you need to create and distribute debian
packages of your software.  That's the idea at least... create an issue if you run into, uh, any issues.

[https://github.com/cfmlprojects/debrepo/issues](https://github.com/cfmlprojects/debrepo/issues) 