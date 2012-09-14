package playRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefAdvertiser.PacketLineOutRefAdvertiser;
import org.eclipse.jgit.transport.UploadPack;
import org.tigris.subversion.javahl.ClientException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.server.dav.DAVServlet;

import play.Logger;
import play.mvc.Http.Response;
import play.mvc.Http.Request;

import models.Project;

import controllers.ProjectApp;

public class RepositoryService {
    public static final String VCS_SUBVERSION = "Subversion";
    public static final String VCS_GIT = "GIT";

    public static Map<String, String> vcsTypes() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(VCS_GIT, "project.new.vcsType.git");
        map.put(VCS_SUBVERSION, "project.new.vcsType.subversion");
        return map;
    }

    public static void deleteRepository(String userName, String projectName, String type)
            throws IOException, ServletException {
        Project project = ProjectApp.getProject(userName, projectName);
        RepositoryService.getRepository(project).delete();
    }

    public static void createRepository(Project project) throws IOException, ServletException,
            ClientException, UnsupportedOperationException {
        RepositoryService.deleteRepository(project.owner, project.name, project.vcs);
        RepositoryService.getRepository(project).create();
    }

    public static ObjectNode getMetaDataFrom(String userName, String projectName, String path)
            throws NoHeadException, UnsupportedOperationException, IOException, GitAPIException,
            ServletException, SVNException {
        Project project = ProjectApp.getProject(userName, projectName);
    	Logger.info(project.vcs);
        return RepositoryService.getRepository(project).findFileInfo(path);
    }

    /**
     * Raw 소스를 보여주는 코드
     * @param userName
     * @param projectName
     * @param path
     * @return
     * @throws ServletException 
     * @throws IOException 
     * @throws UnsupportedOperationException 
     * @throws AmbiguousObjectException 
     * @throws IncorrectObjectTypeException 
     * @throws MissingObjectException 
     * @throws SVNException 
     */
    public static byte[] getFileAsRaw(String userName, String projectName, String path)
            throws MissingObjectException, IncorrectObjectTypeException, AmbiguousObjectException,
            UnsupportedOperationException, IOException, ServletException, SVNException {
        Project project = ProjectApp.getProject(userName, projectName);
        return RepositoryService.getRepository(project).getRawFile(path);
    }

    public static PlayRepository getRepository(Project project) throws IOException,
            ServletException, UnsupportedOperationException {
        if (project.vcs.equals(VCS_GIT)) {
            return new GitRepository(project.owner, project.name);
        } else if (project.vcs.equals(VCS_SUBVERSION)) {
            return new SVNRepository(project.owner, project.name);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static DAVServlet createDavServlet(final String userName) throws ServletException {
        DAVServlet servlet = new DAVServlet();
        servlet.init(new ServletConfig() {

            @Override
            public String getInitParameter(String name) {
                if (name.equals("SVNParentPath")) {
                    return new File(SVNRepository.REPO_PREFIX + userName + "/").getAbsolutePath();
                } else {
                    return play.Configuration.root().getString("application." + name);
                }
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ServletContext getServletContext() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getServletName() {
                throw new UnsupportedOperationException();
            }

        });

        return servlet;
    }

    public static Repository createGitRepository(String userName, String projectName) throws IOException {
        return GitRepository.createGitRepository(userName, projectName);
    }

    public static byte[] gitAdvertise(String userName, String projectName, String service, Response response) throws IOException {
        response.setContentType("application/x-" + service + "-advertisement");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PacketLineOut packetLineOut = new PacketLineOut(byteArrayOutputStream);
        packetLineOut.writeString("# service=" + service + "\n");
        packetLineOut.end();
        PacketLineOutRefAdvertiser packetLineOutRefAdvertiser = new PacketLineOutRefAdvertiser(packetLineOut);

        Repository repository = createGitRepository(userName, projectName);

        if (service.equals("git-upload-pack")) {
            UploadPack uploadPack = new UploadPack(repository);
            uploadPack.setBiDirectionalPipe(false);
            uploadPack.sendAdvertisedRefs(packetLineOutRefAdvertiser);
        } else if (service.equals("git-receive-pack")) {
            ReceivePack receivePack = new ReceivePack(repository);
            receivePack.sendAdvertisedRefs(packetLineOutRefAdvertiser);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gitRpc(String userName, String projectName, String service, Request request, Response response) throws IOException {
        response.setContentType("application/x-" + service + "-result");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // FIXME 스트림으로..
        byte[] buf = request.body().asRaw().asBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(buf);

        Repository repository = createGitRepository(userName, projectName);

        if (service.equals("git-upload-pack")) {
            UploadPack uploadPack = new UploadPack(repository);
            uploadPack.setBiDirectionalPipe(false);
            uploadPack.upload(in, byteArrayOutputStream, null);
        } else if (service.equals("git-receive-pack")) {
            ReceivePack receivePack = new ReceivePack(repository);
            receivePack.setBiDirectionalPipe(false);
            receivePack.receive(in, byteArrayOutputStream, null);
        }

        // receivePack.setEchoCommandFailures(true);//git버전에 따라서 불린값 설정필요.
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

}
