/*********************************************************************
* Copyright (c) 2021 Läubisoft GmbH and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package de.laeubisoft.osgitoolbox.ui.container;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import de.laeubisoft.osgitoolbox.core.container.OSGiClasspathContainer;

@SuppressWarnings("restriction")
public class OSGiClasspathContainerPage extends WizardPage implements IClasspathContainerPage {

	private Label label;
	private IFeatureModel selected;
	private boolean isTest;
	private Button testDependecyButton;

	public OSGiClasspathContainerPage() {
		super("OSGiClasspathContainerPage");
		setImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setTitle("OSGi Classpath Container");
		setDescription("Choose a feature to add its bundles to the classpath");
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button button = new Button(composite, SWT.PUSH);
		button.setText("Select Feature ...");
		// TODO support selection of version ranges...
		SelectionListener listener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFeature();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		button.addSelectionListener(listener);
		testDependecyButton = new Button(composite, SWT.CHECK);
		testDependecyButton.setText("Visible only for test sources");
		testDependecyButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				isTest = testDependecyButton.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		updateUI();
		setControl(composite);
		if (selected == null) {
			setPageComplete(false);
			Display display = Display.getCurrent();
			display.asyncExec(() -> {
				while (display.readAndDispatch()) {
					continue;
				}
				updateFeature();
			});
		}
	}

	private void updateFeature() {
		FeatureModelManager mm = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] allModels = mm.getModels();
		FeatureSelectionDialog dialog = new FeatureSelectionDialog(getShell(), allModels, false);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			for (Object object : models) {
				selected = (IFeatureModel) object;
				updateUI();
			}
		}
		setPageComplete(selected != null);
	}

	private void updateUI() {
		if (label != null) {
			if (selected == null) {
				label.setText("Please choose a feature...");
			} else {
				label.setText(selected.getFeature().getFeature().getId());
			}
		}
		if (testDependecyButton != null) {
			testDependecyButton.setSelection(isTest);
		}
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public IClasspathEntry getSelection() {
		if (selected != null) {
			// TODO support version ranges...
			IClasspathEntry entry = JavaCore
					.newContainerEntry(new Path(OSGiClasspathContainer.ID).append(selected.getFeature().getId()),
							ClasspathEntry.NO_ACCESS_RULES,
							new IClasspathAttribute[] {
									JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, String.valueOf(isTest)) },
							false);
			return entry;
		}
		return null;
	}

	@Override
	public void setSelection(IClasspathEntry containerEntry) {
		if (containerEntry != null) {
			selected = OSGiClasspathContainer.getModel(containerEntry.getPath());
			isTest = containerEntry.isTest();
		}
		setPageComplete(selected != null);
		updateUI();
	}

}
