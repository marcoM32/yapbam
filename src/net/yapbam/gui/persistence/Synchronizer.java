package net.yapbam.gui.persistence;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;

import net.astesana.ajlib.swing.worker.WorkInProgressFrame;
import net.astesana.ajlib.swing.worker.Worker;

public class Synchronizer {
	public static Synchronizer INSTANCE = new Synchronizer();
	
	// The synchronization scheme implemented in this class do not see if the local cache is based on a previous version of the remote data.
	// For instance here is a scenario that explains the previous sentence:
	// 1 - User A modifies the data locally, but do not synchronize with the remote host
	// 2 - User B modifies the data and synchronizes with the remote host
	// 3 - User A saves the data to the remote host ... a override B work with no warning at all :-(
	// A cool functionality could be to inform the user that there's a conflict ?
	// Maybe, this is just a developer perspective, far from what my gran'mother can understand ...
	
	protected Synchronizer() {}
	
	private class SynchroWorker extends Worker<SynchronizationState, Void> implements Cancellable {
		private URI uri;
		SynchroWorker(URI uri) {
			this.uri = uri;
		}
		@Override
		protected SynchronizationState doInBackground() throws Exception {
			setPhase("Synchronizing", -1);
			return backgroundSynchronize(uri, this);
		}
		@Override
		public void cancel() {
			super.cancel(false);
		}
		@Override
		public void reportProgress(int progress) {
			super.reportProgress(progress);
		}
		@Override
		public void setPhase(String phase, int length) {
			super.setPhaseLength(length);
		}
	}

	public File synchronize(Window owner, final URI uri) throws ExecutionException {
		final RemotePersistencePlugin plugin = (RemotePersistencePlugin) PersistenceManager.MANAGER.getPlugin(uri);
		File localCacheFile = plugin.getLocalFile(uri);
		Worker<SynchronizationState, Void> synchroWorker = new SynchroWorker(uri);
		WorkInProgressFrame waitFrame = new WorkInProgressFrame(owner, "Synchronizing", ModalityType.APPLICATION_MODAL, synchroWorker);
		waitFrame.setSize(250, waitFrame.getSize().height);
		waitFrame.setVisible(true);
		if (synchroWorker.isCancelled()) {
			if (!plugin.getLocalFile(uri).exists()) localCacheFile = null;
			//FIXME Buttons are not localized
			if (JOptionPane.showConfirmDialog(owner, "You cancelled to synchronization. Would you like to open the cached data ?", "Synchronization was cancelled", JOptionPane.YES_NO_OPTION)!=0) {
				return null;
			}
		} else {
			SynchronizationState state; 
			try {
				state = synchroWorker.get();
			} catch (InterruptedException e) {
				throw new ExecutionException(e);
			}
			if (state.equals(SynchronizationState.LOCAL_IS_NEWER)) {
				String message = "<html>The data stored on your computer is newer than the one on Dropbox.<br>What do you want to do ?</html>";
				Object[] options = {"Upload computer data to Dropbox", "Revert to the Dropbox content", "Cancel"};
				int n = JOptionPane.showOptionDialog(owner, message, "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
						null, options, options[2]);
				if (n==2) {
					localCacheFile = null;
				} else if (n==0) {
					//FIXME upload();
				} else {
					//FIXME plugin.download(uri);
				}
			} else if (state.equals(SynchronizationState.REMOTE_NOT_FOUND)) {
				String message = "<html>That file doesn't exist anymore on Dropbox.<br>.<br>What do you want to do ?</html>";
				Object[] options = {"Upload computer data to Dropbox", "Delete data on the computer", "Cancel"};
				int n = JOptionPane.showOptionDialog(owner, message, "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
						null, options, options[2]);
				if (n==2) {
					localCacheFile = null;
				} else if (n==0) {
					//FIXME upload();
				} else {
					//FIXME plugin.download(uri);
				}
			}
		}
		return localCacheFile;
	}

	/** Synchronizes local cache with remote resource.
	 * <br>This method is guaranteed to not call any of the Swing API, so it could typically used as a Worker doInBackground task.
	 * @param uri The remote URI
	 * @return The synchronization state
	 * @throws FileNotFoundException if neither the remote resource nor its cache file does exist 
	 * @throws IOException if an exception occurs while synchronizing
	 */
	private SynchronizationState backgroundSynchronize(URI uri, Cancellable task) throws IOException {
		RemotePersistencePlugin plugin = (RemotePersistencePlugin) PersistenceManager.MANAGER.getPlugin(uri);
		Long remoteDate = plugin.getRemoteDate(uri);
		String remoteRevision = plugin.getRemoteRevision(uri);
		String localRevision = plugin.getLocalBaseRevision(uri);
System.out.println("remote rev: "+remoteRevision+", local rev:"+localRevision);
		File file = plugin.getLocalFile(uri);
		if (remoteDate==null && !file.exists()) throw new FileNotFoundException();
		if (remoteDate==null) return SynchronizationState.REMOTE_NOT_FOUND;
		Long dateFile = file.exists()?file.lastModified():0L;
		if (dateFile.equals(remoteDate)) {
			// The file date is identical to remote version
			return SynchronizationState.SYNCHRONIZED;
		} else if (dateFile.compareTo(remoteDate)<0) {
			// The file is older than remote version
			// Download the remote version
			download(uri, task);
			return SynchronizationState.SYNCHRONIZED;
		} else {
			// The file is newer than remote version
			return SynchronizationState.LOCAL_IS_NEWER;
		}
	}

	private void download(URI uri, Cancellable task) throws IOException {
		//FIXME Do not download directly to the target file, it will be corrupted if copy fails !!!
		RemotePersistencePlugin plugin = (RemotePersistencePlugin) PersistenceManager.MANAGER.getPlugin(uri);
		System.out.println ("downloading "+uri);
		File file = plugin.getLocalFile(uri);
		file.getParentFile().mkdirs();
		Long remoteDate = plugin.getRemoteDate(uri);
		String revision = plugin.getRemoteRevision(uri);
		plugin.download(uri, file, task);
		file.setLastModified(remoteDate);
		plugin.setLocalBaseRevision(uri, revision);
	}

	private void upload(URI uri, Cancellable task) throws IOException {
		RemotePersistencePlugin plugin = (RemotePersistencePlugin) PersistenceManager.MANAGER.getPlugin(uri);
		System.out.println ("uploading "+uri);
		File file = plugin.getLocalFile(uri);
		plugin.upload(file, uri, task);
		Long remoteDate = plugin.getRemoteDate(uri);
		file.setLastModified(remoteDate);
	}
}
