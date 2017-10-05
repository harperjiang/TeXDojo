package edu.uchicago.cs.hao.texdojo.latexeditor.editors.assistant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import edu.uchicago.cs.hao.texdojo.latexeditor.Activator;
import edu.uchicago.cs.hao.texdojo.latexeditor.preferences.PreferenceConstants;

/**
 * This Strategy align the current line to the previous line, and auto-break
 * lines when it's too long (configurable)
 * 
 * @author harper
 *
 */
public class LineAlignStrategy implements IAutoEditStrategy {

	static Pattern leadingSpace = Pattern.compile("^([\t ]+)[^\t ].*");
	static String crlf = System.getProperty("line.separator");

	int lineWrapLimit = -1;
	long lastFetch = -1;

	Map<IDocument, List<IRegion>> insertedCarriages = new HashMap<IDocument, List<IRegion>>();

	public int getLineWrapLimit() {
		long current = System.currentTimeMillis();
		if (current - lastFetch > 1000) {
			IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
			lineWrapLimit = prefs.getInt(PreferenceConstants.P_LINE_WRAP, -1);
		}
		return lineWrapLimit;
	}

	@Override
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		if (command.text.equals("\n")) {
			// Look for the start of the line
			int start = command.offset - 1;
			try {
				// Look for previous line
				IRegion prevLine = document.getLineInformationOfOffset(start);
				String lineContent = document.get(prevLine.getOffset(), prevLine.getLength());

				Matcher matcher = leadingSpace.matcher(lineContent);
				if (matcher.matches()) {
					String leading = matcher.group(1);
					String text = MessageFormat.format("\n{0}", leading);
					command.text = text;
				}
			} catch (BadLocationException e) {
				// Do nothing
			}
		} else {
			// Line Auto-wrap
			List<IRegion> carriages = insertedCarriages.get(document);
			if (carriages == null) {
				carriages = new ArrayList<IRegion>();
				insertedCarriages.put(document, carriages);
			}

			int lwm = getLineWrapLimit();
			if (lwm != -1) {
				try {
					int textLength = command.text.length();
					IRegion currentLine = document.getLineInformationOfOffset(command.offset);
					if (currentLine.getLength() + textLength > lwm) {
						// Remove and recalculate all carriages generated by us after the insertion
						// point
						int remainingStart = command.offset;
						int lineOffset = command.offset - currentLine.getOffset();
						StringBuilder remainingText = new StringBuilder();

						int docOffset = command.offset;
						int docLength = document.getLength() - command.offset;
						remainingText.append(document.get(docOffset, docLength));
						Iterator<IRegion> ite = carriages.iterator();
						while (ite.hasNext()) {
							IRegion region = ite.next();
							if (region.getOffset() > remainingStart) {
								// Replace the carriage with space and remove it
								ite.remove();
								remainingText.replace(region.getOffset(), region.getLength(), " ");
							}
						}
						// Insert the text to be shown
						remainingText.insert(0, command.text);

						carriages.addAll(wrapText(remainingText, remainingStart, lineOffset, lwm));
						document.replace(docOffset, docLength, remainingText.toString());

						command.text = "";
					}

				} catch (BadLocationException e) {
				}
			}
		}
	}

	public static List<IRegion> wrapText(StringBuilder text, int globalOffset, int lineOffset, int limit) {
		List<IRegion> replaced = new ArrayList<IRegion>();

		int current = lineOffset;
		int lastSpaceOffset = -1;
		int lastSpaceLength = -1;
		boolean inSpace = false;

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			current++;
			if (current > limit) {
				if (lastSpaceOffset != -1) {
					replaced.add(new Region(globalOffset + lastSpaceOffset, lastSpaceLength));
					text.replace(lastSpaceOffset, lastSpaceOffset + lastSpaceLength - 1, LineAlignStrategy.crlf);
					i -= lastSpaceLength - LineAlignStrategy.crlf.length();
					current = 0;
				}
			}
			switch (ch) {
			case '\n':
			case '\r':
				current = 0;
				inSpace = false;
				break;
			case '\t':
			case ' ':
				if (!inSpace) {
					inSpace = true;
					lastSpaceOffset = i;
				}
				break;
			default:
				if (inSpace) {
					inSpace = false;
					lastSpaceLength = i - lastSpaceOffset;
				}
				break;
			}
		}

		return replaced;
	}
}