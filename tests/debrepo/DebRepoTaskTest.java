package debrepo;

import java.io.File;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.Test;
import org.vafer.jdeb.ant.DebAntTask;

import debrepo.ant.DebRepoTask;
import static org.junit.Assert.*;

public class DebRepoTaskTest extends TestBase {

	
    private Project project;

	@Test
	public void testRunTestBuildFile() throws Exception {
	    runBuildFileTarget(new File( getFileResource( "/../../build/build.xml")),"test2");
	}
	
	protected DebRepoTask createBasicTask( File dir ) {
	    DebRepoTask task = new DebRepoTask();
		task.setProject( createProject() );
        task.setDebsDir(dir);
        task.setDescription("test repo");
        task.setVersion("1.0");
		return task;
	}

	protected void runBuildFileTarget( File buildFile , String target) {
	    BuildLogger logger = new DefaultLogger();
	    logger.setOutputPrintStream(System.out);
	    logger.setErrorPrintStream(System.out);
	    logger.setMessageOutputLevel(Project.MSG_INFO);

	    project = new Project();
	    project.addBuildListener(logger);
        project.setCoreLoader(getClass().getClassLoader());
        project.init();
        project.setBaseDir(buildFile.getParentFile());

        ProjectHelper.configureProject(project, buildFile);
        String targetToExecute = (target != null && target.trim().length() > 0) ? target.trim() : project.getDefaultTarget();
        project.executeTarget(targetToExecute);
        project.fireBuildFinished(null);
        
	}
	
	protected DebAntTask createBasicDebAntTask( File dir ) {
	    DebAntTask task = new DebAntTask();
	    task.setProject( createProject() );
	    return task;
	}
	
	protected DebRepoTask createBasicTaskWithFiles( File dir ) {
	    DebRepoTask task = new DebRepoTask();
	    task.setProject( createProject() );
	    
	    task.setDebsDir(dir);
	    task.setDescription("test repo");
        task.setVersion("1.0");

	    return task;
	}
	
	private Project createProject() {
		Project project = new Project();
		project.setCoreLoader(getClass().getClassLoader());
		project.init();
		return project;
	}

	protected File ensureTargetDir() {
		File dir = new File("target");
		if (!dir.exists()) {
			assertTrue(dir.mkdir());
		}
		return dir;
	}

}
