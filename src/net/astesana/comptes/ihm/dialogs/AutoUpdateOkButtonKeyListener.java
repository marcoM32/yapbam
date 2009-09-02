package net.astesana.comptes.ihm.dialogs;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/** Utility KeyListener that calls the updateOkButtonEnabled of a dialog each
 * time a key is released in a textComponent listened by this class.
 * @author Jean-Marc
 */
class AutoUpdateOkButtonKeyListener extends KeyAdapter {
	private AbstractDialog dialog;
	
	AutoUpdateOkButtonKeyListener (AbstractDialog dialog) {
		this.dialog = dialog;
	}
	public void keyReleased(KeyEvent e) {
		dialog.updateOkButtonEnabled();
	}
}