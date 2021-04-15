/*********************************************************************
* Copyright (c) 2021 Läubisoft GmbH and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package de.laeubisoft.osgitoolbox.core.container;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.PluginModelManager;

@SuppressWarnings("restriction")
public class OSGiClasspathContainerInitializer extends ClasspathContainerInitializer {

	public OSGiClasspathContainerInitializer() {
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IProject project = javaProject.getProject();
		if (!manager.isInitialized()) {
			Job job = Job.create(PDECoreMessages.PluginModelManager_InitializingPluginModels, monitor -> {
				if (!PDECore.getDefault().getModelManager().isInitialized()) {
					PDECore.getDefault().getModelManager().targetReloaded(monitor);
				}
			});
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
			}
		}
		if (project.exists() && project.isOpen()) {
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { javaProject },
					new IClasspathContainer[] {
							new OSGiClasspathContainer(containerPath, ClasspathUtilCore.getBuild(model)) },
					null);
		}
	}

}
