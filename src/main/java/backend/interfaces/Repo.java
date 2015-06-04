package backend.interfaces;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.eclipse.egit.github.core.Comment;

import backend.UserCredentials;
import backend.resource.TurboIssue;
import backend.resource.TurboLabel;
import backend.resource.TurboMilestone;
import backend.resource.TurboUser;
import github.TurboIssueEvent;

public interface Repo {

	public boolean login(UserCredentials credentials);

	public List<TurboIssue> getIssues(String repoId);
	public List<TurboLabel> getLabels(String repoId);
	public List<TurboMilestone> getMilestones(String repoId);
	public List<TurboUser> getCollaborators(String repoId);

	// Returns tuples in order to be maximally generic
	public ImmutableTriple<List<TurboIssue>, String, Date>
		getUpdatedIssues(String repoId, String eTag, Date lastCheckTime);
	public ImmutablePair<List<TurboLabel>, String> getUpdatedLabels(String repoId, String eTag);
	public ImmutablePair<List<TurboMilestone>, String> getUpdatedMilestones(String repoId, String eTag);
	public ImmutablePair<List<TurboUser>, String> getUpdatedCollaborators(String repoId, String eTag);

	public List<TurboIssueEvent> getEvents(String repoId, int issueId);
	public List<Comment> getComments(String repoId, int issueId);

	public boolean isRepositoryValid(String repoId);
}
