/*
 * Copyright (C) 2024 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole.util;

/**
 *
 * @author AwesomeTMC
 */
public class UIUtil {
    /**
     * Convert plaintext to html for multiline support in JLabels.
     * @param text The text to convert.
     * @return The text, converted to HTML.
     */
    public static String textToHTML(String text)
    {
        text = "<html><p>" + text + "</p></html>";
        text = text.replace("\n", "<br>");
        return text;
    }
    
    /**
     * Adds new line characters according to the limit of each line.
     * @param text The text to line wrap.
     * @param softLimit Lets words continue past this limit, but if a space is found, it is replaced with a new line.
     * @param hardLimit Words cannot continue past this limit, and will be cut off if they do.
     * @return The line wrapped text.
     */
    
    public static String lineWrapText(String text, int softLimit, int hardLimit)
    {
        StringBuilder newStr = new StringBuilder();
        int charNum = 0;
        for (char c : text.toCharArray())
        {
            if (charNum > softLimit)
            {
                switch (c) {
                    case '/':
                        newStr.append('\n');
                        charNum = 0;
                        break;
                    case ' ':
                        c = '\n';
                        break;
                }
            }
            if (c == '\n')
            {
                charNum = 0;
                newStr.append(c);
                continue;
            }
            else if (charNum > hardLimit) {
                newStr.append('\n');
                charNum = 0;
            }
            
            newStr.append(c);
            charNum++;
            
        }
        return newStr.toString();
    }
    
    /**
     * Converts HTML text to plain text. Like textToHTML but in reverse.
     * @param html The HTML text to convert.
     * @return The plain text converted from HTML.
     */
    public static String HTMLToText(String html)
    {
        html = html.replace("<br>", "\n");
        // replace any remaining tags with nothing
        html = html.replaceAll("<[A-Za-z]+>", "");
        html = html.replaceAll("</[A-Za-z]+>", "");
        return html;
    }
    
    /**
     * Adds new line characters according to the limit of each line and then converts it to HTML text.
     * @param text The text to line wrap and HTMLify.
     * @param softLimit Lets words continue past this limit, but if a space is found, it is replaced with a new line.
     * @param hardLimit Words cannot continue past this limit, and will be cut off if they do.
     * @return The line wrapped text converted to HTML.
     */
    public static String textToHTMLLineWrap(String text, int softLimit, int hardLimit)
    {
        return textToHTML(lineWrapText(text, softLimit, hardLimit));
    }
}
