package ui;

import java.util.ArrayList;

import model.Model;
import model.TurboLabel;

public class TurboLabelGroup implements LabelTreeItem {
	
	private String name;
	private ArrayList<TurboLabel> labels = new ArrayList<>();
	
	public TurboLabelGroup(String name) {
		this.name = name;
	}
	
	public void addLabel(TurboLabel label) {
		labels.add(label);
		label.setGroup(name);
	}
	
	public String getValue() {
		return name;
	}
	public void setValue(String value) {
		this.name = value;
		for (TurboLabel label : labels) {
			label.setGroup(value);
		}
	}

	@Override
	public String toString() {
		return "TurboLabelGroup [name=" + name + ", labels=" + labels + "]";
	}

	public ArrayList<TurboLabel> getLabels() {
		return new ArrayList<TurboLabel>(labels);
	}
}
