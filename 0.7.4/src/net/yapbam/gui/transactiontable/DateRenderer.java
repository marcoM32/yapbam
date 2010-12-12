package net.yapbam.gui.transactiontable;

import java.text.SimpleDateFormat;

import net.yapbam.gui.LocalizationData;

public class DateRenderer extends ObjectRenderer {
	private static final long serialVersionUID = 1L;
	
	public DateRenderer () {
		super();
		this.setHorizontalAlignment(CENTER);
	}

	@Override
    public void setValue(Object value) {
		setText(value==null?"":SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, LocalizationData.getLocale()).format(value));
    }
}
