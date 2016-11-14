/*******************************************************************************
 * Copyright (c) Oct 2016 Hao Jiang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hao Jiang - initial API and implementation and/or initial documentation
 *******************************************************************************/

package edu.uchicago.cs.hao.texdojo.latexeditor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import edu.uchicago.cs.hao.texdojo.latexeditor.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public static final String DEFAULT_LATEX_EXE = "pdflatex";

	public static final String DEFAULT_BIB_EXE = "bibtex";

	public static final String DEFAULT_TEMP_FILE = "*.aux,*.log";

	public static final boolean DEFAULT_COMPILE_DOCUMENT = true;

	public static final String DEFAULT_MAIN_TEX = "main.tex";

	public static final String DEFAULT_COLOR_COMMAND = StringConverter.asString(new RGB(0, 0, 0));

	public static final String DEFAULT_COLOR_ARG = StringConverter.asString(new RGB(0, 0, 0));

	public static final String DEFAULT_COLOR_OPTION = StringConverter.asString(new RGB(0, 0, 0));

	public static final String DEFAULT_COLOR_COMMENT = StringConverter.asString(new RGB(0, 0, 0));

	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_LATEX_EXE, DEFAULT_LATEX_EXE);
		store.setDefault(PreferenceConstants.P_BIBTEX_EXE, DEFAULT_BIB_EXE);
		store.setDefault(PreferenceConstants.P_TEMP_FILE, DEFAULT_TEMP_FILE);
		store.setDefault(PreferenceConstants.P_COMPILE_DOC, DEFAULT_COMPILE_DOCUMENT);
		store.setDefault(PreferenceConstants.P_MAIN_TEX, DEFAULT_MAIN_TEX);
		store.setDefault(PreferenceConstants.P_COLOR_COMMAND, DEFAULT_COLOR_COMMAND);
		store.setDefault(PreferenceConstants.P_COLOR_COMMENT, DEFAULT_COLOR_COMMENT);
		store.setDefault(PreferenceConstants.P_COLOR_OPTION, DEFAULT_COLOR_OPTION);
		store.setDefault(PreferenceConstants.P_COLOR_ARG, DEFAULT_COLOR_ARG);
	}

}
