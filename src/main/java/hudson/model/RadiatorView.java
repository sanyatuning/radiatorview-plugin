package hudson.model;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;

import java.io.IOException;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A configurable Radiator-Style job view suitable for use in extreme feedback
 * systems - ideal for running on a spare PC in the office. Many thanks to
 * Julien Renaut for the xfpanel plugin that inspired some of the updates to
 * this view.
 * 
 * @author Mark Howard (mh@tildemh.com)
 */
public class RadiatorView extends ListView {
	
	private static final int DEFAULT_CAPTION_SIZE = 36;
	private static final String DEFAULT_GROUP_REGEX = "(.*?)[-_:].*";

	/**
	 * Entries to be shown in the view.
	 */
	private transient Collection<IViewEntry> entries;

	/**
	 * Cache of location of jobs in the build queue.
	 */
	transient Map<hudson.model.Queue.Item, Integer> placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();

	/**
	 * Colours to use in the view.
	 */
	transient ViewEntryColors colors;

	/**
	 * User configuration - show stable builds when there are some unstable
	 * builds.
	 */
	Boolean showStable = false;

	/**
	 * User configuration - show details in stable builds.
	 */
	Boolean showStableDetail = false;

	/**
	 * User configuration - show build stability icon.
	 */
	Boolean showBuildStability = false;

	/**
	 * User configuration - high visibility mode.
	 */
	Boolean highVis = true;

	/**
	 * User configuration - group builds by regex.
	 */
    Boolean groupByRegex = true;

    /**
     * User configuration - group builds by regex.
     */
    String groupRegex;

	/**
	 * User configuration - text for the caption to be used on the radiator's headline.
	 */
	String captionText;
	 
	/**
	 * User configuration - size in points (1pt = 1/72in) for the caption to be used on the radiator's headline.
	 */
	Integer captionSize;

	/**
	 * @param name
	 *            view name.
	 * @param showStable
	 *            if stable builds should be shown.
	 * @param showStableDetail
	 *            if detail should be shown for stable builds.
	 * @param highVis
	 *            high visibility mode.
     * @param groupByRegex
     *            If true, builds will be shown grouped together based on user defined regex.
     * @param groupRegex
     *            regex match groups will be concatenated to project name
	 * @param showBuildStability
	 *            Shows weather icon for job view when true.
	 * @param captionText
	 *            Caption text to be used on the radiator's headline.
	 * @param captionSize
	 *            Caption size for the radiator's headline.
	 */
	@DataBoundConstructor
	public RadiatorView(String name, Boolean showStable,
			Boolean showStableDetail, Boolean highVis, Boolean groupByRegex, String groupRegex,
			Boolean showBuildStability, String captionText, Integer captionSize) {
		super(name);
		this.showStable = showStable;
		this.showStableDetail = showStableDetail;
		this.highVis = highVis;
		this.groupByRegex = groupByRegex;
		this.groupRegex = groupRegex == null ? DEFAULT_GROUP_REGEX : groupRegex;
		this.showBuildStability = showBuildStability;
		this.captionText = captionText;
		this.captionSize = captionSize;
	}
	
	public RadiatorView(String name)
	{
		super(name);
	}

	/**
	 * @return the colors to use
	 */
	public ViewEntryColors getColors() {
		if (this.colors == null) {
			this.colors = ViewEntryColors.DEFAULT;
		}
		return this.colors;
	}

	public ProjectViewEntry getContents() {
		ProjectViewEntry contents = new ProjectViewEntry();

		placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();
		int j = 1;
		for (hudson.model.Queue.Item i : Hudson.getInstance().getQueue()
				.getItems()) {
			placeInQueue.put(i, j++);
		}

		for (TopLevelItem item : super.getItems()) {
			if (item instanceof AbstractProject) {
				AbstractProject project = (AbstractProject) item;
				if (!project.isDisabled()) {
					IViewEntry entry = new JobViewEntry(this, project);
					contents.addBuild(entry);
				}
			}
		}

		return contents;
	}
	
	public ProjectViewEntry getContentsByPrefix()
	{
		ProjectViewEntry contents = new ProjectViewEntry();
		ProjectViewEntry allContents = getContents();
		Map<String, ProjectViewEntry> jobsByPrefix = new HashMap<String, ProjectViewEntry>();
		
		for (IViewEntry job: allContents.getJobs())
		{
			String prefix = getPrefix(job.getName());
			ProjectViewEntry project = jobsByPrefix.get(prefix);
			if (project == null)
			{
				project = new ProjectViewEntry(prefix);
				jobsByPrefix.put(prefix, project);
				contents.addBuild(project);
			}
			project.addBuild(job);
		}
		return contents;
	}

	private String getPrefix(String name)
    {
        Pattern pattern = Pattern.compile(groupRegex);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            return "No Project";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            sb.append(matcher.group(i));
        }
        return sb.toString();
	}


	/**
	 * Gets from the request the configuration parameters
	 * 
	 * @param req
	 *            {@link StaplerRequest}
	 * @throws ServletException
	 *             if any
	 * @throws FormException
	 *             if any
	 */
	@Override
	protected void submit(StaplerRequest req) throws ServletException, IOException,
			FormException {
		super.submit(req);
		this.showStable = Boolean.parseBoolean(req.getParameter("showStable"));
		this.showStableDetail = Boolean.parseBoolean(req
				.getParameter("showStableDetail"));
		this.highVis = Boolean.parseBoolean(req.getParameter("highVis"));
        this.groupByRegex = req.getParameter("groupByRegex") != null;
        if (this.groupByRegex) {
            String param = req.getParameter("groupRegex");
            this.groupRegex = param.isEmpty() ? DEFAULT_GROUP_REGEX : param;
        }
		this.showBuildStability = Boolean.parseBoolean(req.getParameter("showBuildStability"));
        this.captionText = req.getParameter("captionText");
        try {
			this.captionSize = Integer.parseInt(req.getParameter("captionSize"));
		} catch (NumberFormatException e) {
			this.captionSize = DEFAULT_CAPTION_SIZE;
		}
		
	}

	public Boolean getShowStable() {
		return showStable;
	}

	public Boolean getShowStableDetail() {
		return showStableDetail;
	}

	public Boolean getHighVis() {
		return highVis;
	}
	
	public Boolean getGroupByRegex()
	{
		return groupByRegex;
	}

    public String getGroupRegex() {
        return groupRegex;
    }

	public Boolean getShowBuildStability() {
		return showBuildStability;
	}

	public String getCaptionText() {
		return captionText;
	}

	public Integer getCaptionSize() {
		return captionSize;
	}
	/**
	 * Converts a list of jobs to a list of list of jobs, suitable for display
	 * as rows in a table.
	 * 
	 * @param jobs
	 *            the jobs to include.
	 * @param failingJobs
	 *            if this is a list of failing jobs, in which case fewer jobs
	 *            should be used per row.
	 * @return a list of fixed size view entry lists.
	 */
	public Collection<Collection<IViewEntry>> toRows(Collection<IViewEntry> jobs,
			Boolean failingJobs) {
		int jobsPerRow = 1;
		if (failingJobs) {
			if (jobs.size() > 3) {
				jobsPerRow = 2;
			}
			if (jobs.size() > 9) {
				jobsPerRow = 3;
			}
			if (jobs.size() > 15) {
				jobsPerRow = 4;
			}
		} else {
			// don't mind having more rows as much for passing jobs.
			jobsPerRow = (int) Math.floor(Math.sqrt(jobs.size()) / 1.5);
		}
		Collection<Collection<IViewEntry>> rows = new ArrayList<Collection<IViewEntry>>();
		Collection<IViewEntry> current = null;
		int i = 0;
		for (IViewEntry job : jobs) {
			if (i == 0) {
				current = new ArrayList<IViewEntry>();
				rows.add(current);
			}
			current.add(job);
			i++;
			if (i >= jobsPerRow) {
				i = 0;
			}
		}
		return rows;
	}

    /**
     * @param passing
     * @param failing
     * @return
     */
    public Collection<IViewEntry> getJobs(Collection<IViewEntry> passing, Collection<IViewEntry> failing) {
        Collection<IViewEntry> aggregate = new TreeSet<IViewEntry>(new EntryComparator());
        aggregate.addAll(failing);
        if (showStable) {
            aggregate.addAll(passing);
        }
        if (aggregate.isEmpty()) {
            ProjectViewEntry view = new ProjectViewEntry(getDisplayName());
            for (IViewEntry v : passing) {
                view.addBuild(v);
            }
            aggregate.add(view);
        }
        return aggregate;
    }


	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		public DescriptorImpl() {
			super(RadiatorView.class);
		}

		@Override
		public String getDisplayName() {
			return "Radiator";
		}

        /**
         * Checks if the include regular expression is valid.
         */
        public FormValidation doCheckIncludeRegex(@QueryParameter String value) {
            String v = Util.fixEmpty(value);
            if (v != null) {
                try {
                    Pattern.compile(v);
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error(pse.getMessage());
                }
            }
            return FormValidation.ok();
        }

        /**
         * Checks if the group regular expression is valid.
         */
        public FormValidation doCheckGroupRegex(@QueryParameter String value) {
            String v = Util.fixEmpty(value);
            if (v != null) {
                try {
                    Pattern.compile(v);
                } catch (PatternSyntaxException pse) {
                    return FormValidation.error(pse.getMessage());
                }
            }
            return FormValidation.ok();
        }
	}
}
