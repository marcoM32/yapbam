package net.yapbam.gui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import net.yapbam.gui.LocalizationData;
import net.yapbam.gui.Preferences;
import net.yapbam.gui.YapbamState;
import net.yapbam.gui.dialogs.update.CheckUpdateDialog;
import net.yapbam.update.VersionManager;
import net.yapbam.util.DateUtils;

/** This class is in charge of checking for Yapbam updates over the Internet */
@SuppressWarnings("serial")
public class CheckNewReleaseAction extends AbstractAction {
	private static final String LAST_UPDATE_CHECK_KEY = "net.yapbam.lastUpdateCheck"; //$NON-NLS-1$
	private Window owner;

	/**
	 * Constructor
	 * 
	 * @param owner
	 *          The parent window of all dialogs that may be opened by this action
	 */
	public CheckNewReleaseAction(Window owner) {
		super(LocalizationData.get("MainMenu.CheckUpdate")); //$NON-NLS-1$
		putValue(SHORT_DESCRIPTION, LocalizationData.get("MainMenu.CheckUpdate.ToolTip")); //$NON-NLS-1$
		this.owner = owner;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		CheckUpdateDialog.check(owner, false, false);
	}

	/**
	 * Checks for updates over the Internet.
	 * 
	 * @param owner
	 *          The parent window of all dialogs that may be opened by this method
	 */
	public static void doAutoCheck(final Window owner) {
		if (Preferences.INSTANCE.isFirstRun()) {
			// Ask the user to grant us the right to connect to Internet to check for updates
			int option = JOptionPane.showOptionDialog(
							owner, LocalizationData.get("MainMenu.CheckUpdate.FirstRun.message"), //$NON-NLS-1$
							LocalizationData.get("MainMenu.CheckUpdate.FirstRun.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, //$NON-NLS-1$
							new String[] { LocalizationData.get("GenericButton.yes"), LocalizationData.get("GenericButton.no") }, LocalizationData.get("GenericButton.yes")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Preferences.INSTANCE.setAutoUpdate(-option, false);
		}
		int lastCheck = DateUtils.dateToInteger(YapbamState.INSTANCE.getDate(LAST_UPDATE_CHECK_KEY));
		int releaseDate = DateUtils.dateToInteger(VersionManager.getVersion().getReleaseDate());
		int today = DateUtils.dateToInteger(new Date());
		int days = Preferences.INSTANCE.getAutoUpdatePeriod();
		boolean prefChoice = (days>=0) && (today - lastCheck >= days); // Auto check is requested by preference settings
		boolean forced = !Preferences.INSTANCE.isFirstRun() && (today - Math.max(releaseDate,lastCheck)>30); // Force checking because release is too old
		if (prefChoice || forced) {
			CheckUpdateDialog.check(owner, true, forced);
		}
	}
}