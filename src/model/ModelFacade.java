package model;

import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;

public class ModelFacade {
	
	private GitHubClient client = new GitHubClient();
	private IRepositoryIdProvider repoId = null;
	private IssueManager issueManager = new IssueManager();
	private AuthenticationManager authManager = new AuthenticationManager(client);
	private MilestoneManager milestoneManager = new MilestoneManager();
	private LabelManager labelManager = new LabelManager();
	private CollaboratorManager colManager = new CollaboratorManager(client);
	
	public boolean login(String userId, String password) {
		return authManager.login(userId, password);
	}
	
	public void setRepository(String owner, String repository) {
		if (owner != null) {
			repoId = RepositoryId.create(owner, repository);
		} else {
			repoId = RepositoryId.create(client.getUser(), repository);
		}
	}
	
	public IssueManager getIssueManager() {
		return this.issueManager;
	}
	
	public LabelManager getLabelManager() {
		return this.labelManager;
	}
	
	public MilestoneManager getMilestoneManager() {
		return this.milestoneManager;
	}
	
	public List<TurboCollaborator> getCollaborators() {
		return colManager.getAllCollaborators(repoId);
	}
	
}
