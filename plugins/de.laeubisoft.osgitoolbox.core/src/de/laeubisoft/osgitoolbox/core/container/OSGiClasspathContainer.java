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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDEClasspathContainer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@SuppressWarnings("restriction")
public class OSGiClasspathContainer extends PDEClasspathContainer implements IClasspathContainer {

	public static final Comparator<IFeatureModel> HIGHEST_VERSION = Comparator.comparing(IFeatureModel::getFeature,
			Comparator.comparing(IFeature::getVersion, (v1, v2) -> Version.valueOf(v1).compareTo(Version.valueOf(v2))));

	public static final String ID = "de.laeubisoft.osgitoolbox.container.OSGi";

	private IPath containerPath;

	private IClasspathEntry[] entries;

	private IBuild build;

	public OSGiClasspathContainer(IPath containerPath, IBuild build) {
		this.containerPath = containerPath;
		this.build = build;
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		if (entries == null) {
			IFeatureModel featureModel = getModel(getPath());
			IFeaturePlugin[] plugins = featureModel.getFeature().getPlugins();
			entries = Arrays.stream(plugins).map(p -> PluginModelManager.getInstance().findModel(p.getId()))//
					.filter(Objects::nonNull).filter(IPluginModelBase::isEnabled)//
					.flatMap(model -> Arrays
							.stream(new RequiredPluginsClasspathContainer(model, build).getClasspathEntries()))//
					.distinct().toArray(IClasspathEntry[]::new);
		}
		return entries;
	}

	@Override
	public String getDescription() {
		return "OSGi Dependencies";
	}

	@Override
	public int getKind() {
		return K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		return containerPath;
	}

	public static IFeatureModel getModel(IPath path) {
		String[] segments = path.segments();
		if (segments.length > 1) {
			FeatureModelManager mm = PDECore.getDefault().getFeatureModelManager();
			IFeatureModel[] models = mm.findFeatureModels(segments[1]);
			if (segments.length > 2) {
				VersionRange range = VersionRange.valueOf(segments[2]);
				return Arrays.stream(models)
						.filter(m -> range.includes(Version.parseVersion(m.getFeature().getVersion())))
						.max(HIGHEST_VERSION).orElse(null);
			} else {
				return Arrays.stream(models).max(HIGHEST_VERSION).orElse(null);
			}
		}
		return null;
	}

}
