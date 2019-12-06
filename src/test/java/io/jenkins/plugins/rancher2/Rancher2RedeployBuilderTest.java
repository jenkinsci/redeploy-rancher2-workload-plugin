package io.jenkins.plugins.rancher2;

public class Rancher2RedeployBuilderTest {

//    @Rule
//    public JenkinsRule jenkins = new JenkinsRule();
//
//    final String name = "Bobby";
//
//    @Test
//    public void testConfigRoundtrip() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        project.getBuildersList().add(new Rancher2RedeployBuilder(name));
//        project = jenkins.configRoundtrip(project);
//        jenkins.assertEqualDataBoundBeans(new Rancher2RedeployBuilder(name), project.getBuildersList().get(0));
//    }
//
//    @Test
//    public void testConfigRoundtripFrench() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        Rancher2RedeployBuilder builder = new Rancher2RedeployBuilder(name);
//        builder.setUseFrench(true);
//        project.getBuildersList().add(builder);
//        project = jenkins.configRoundtrip(project);
//
//        Rancher2RedeployBuilder lhs = new Rancher2RedeployBuilder(name);
//        lhs.setUseFrench(true);
//        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
//    }
//
//    @Test
//    public void testBuild() throws Exception {
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        Rancher2RedeployBuilder builder = new Rancher2RedeployBuilder(name);
//        project.getBuildersList().add(builder);
//
//        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
//        jenkins.assertLogContains("Hello, " + name, build);
//    }
//
//    @Test
//    public void testBuildFrench() throws Exception {
//
//        FreeStyleProject project = jenkins.createFreeStyleProject();
//        Rancher2RedeployBuilder builder = new Rancher2RedeployBuilder(name);
//        builder.setUseFrench(true);
//        project.getBuildersList().add(builder);
//
//        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
//        jenkins.assertLogContains("Bonjour, " + name, build);
//    }
//
//    @Test
//    public void testScriptedPipeline() throws Exception {
//        String agentLabel = "my-agent";
//        jenkins.createOnlineSlave(Label.get(agentLabel));
//        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
//        String pipelineScript
//                = "node {\n"
//                + "  greet '" + name + "'\n"
//                + "}";
//        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
//        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
//        String expectedString = "Hello, " + name + "!";
//        jenkins.assertLogContains(expectedString, completedBuild);
//    }

}