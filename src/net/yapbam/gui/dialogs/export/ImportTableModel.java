package net.yapbam.gui.dialogs.export;

import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.yapbam.gui.LocalizationData;

@SuppressWarnings("serial")
public class ImportTableModel extends AbstractTableModel {
	private boolean[] linked;
	private int[] to;
	private String[] fields;
	
	public ImportTableModel() {
		super();
		this.fields = new String[0];
		this.linked = new boolean[ExportTableModel.columns.length];
		this.to = new int[this.linked.length];
		for (int i = 0; i < this.linked.length; i++) {
			this.to[i] = -1;
		}
	}
	
	public int[] getRelations() {
		int[] result = new int[ExportTableModel.columns.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = linked[i]?to[i]:-1;
		}
		return result;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getRowCount() {
		return ExportTableModel.columns.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex==0) {
			return ExportTableModel.columns[rowIndex];
		} else if (columnIndex==1) {
			return linked[rowIndex] && (to[rowIndex]>=0);
		} else {
			int index = this.to[rowIndex];
			return ((index==-1) || !linked[rowIndex])?"-":(fields.length>index?fields[index]:"");
		}
	}

	@Override
	public String getColumnName(int column) {
		if (column==0) return LocalizationData.get("ImportDialog.YapbamFields");
		else if (column==1) return LocalizationData.get("ImportDialog.linkedTo");
		else return LocalizationData.get("ImportDialog.importedFields");
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex!=0;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex==1) this.linked[rowIndex] = (Boolean) aValue;
		if (columnIndex==2) {
			this.to[rowIndex] = -1;
			for (int i = 0; i < fields.length; i++) {
				if (aValue == fields[i]) {
					this.to[rowIndex] = i;
					this.linked[rowIndex] = true;
					break;
				}
			}
		}
		fireTableRowsUpdated(rowIndex, rowIndex);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex==1) return Boolean.class;
		return super.getColumnClass(columnIndex);
	}
	
	public void setFields(String[] fields) {
		this.fields = fields;
		// Auto select fields equals to the columns title
		List<String> asList = Arrays.asList(fields);
		for (int i = 0; i < ExportTableModel.columns.length; i++) {
			int index = asList.indexOf(ExportTableModel.columns[i]);
			if (index>=0) {
				to[i] = index;
				linked[i] = true;
			}
		}
		this.fireTableDataChanged();
	}
}
