/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  --- CVS Information ---
 *  $Id: GeneralTextPane.java,v 1.7 2005/10/03 00:18:26 gregork Exp $
 */
package phex.gui.dialogs.options;

import phex.gui.common.HTMLMultiLinePanel;
import phex.utils.Localizer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class GeneralTextPane extends OptionsSettingsPane
{
    private String headerText;
    private String contentText;

    public GeneralTextPane( String aHeaderText, String aContentText,
        String treeRepresentation )
    {
        super( treeRepresentation );
        headerText = aHeaderText;
        contentText = aContentText;
    }

    protected void prepareComponent()
    {
        FormLayout layout = new FormLayout(
            "10dlu, 250dlu:grow, 2dlu", // columns
            "p, 3dlu, p" );// rows 
        
        setLayout( layout );
        
        PanelBuilder builder = new PanelBuilder( layout, this );
        CellConstraints cc = new CellConstraints();
        
        builder.addSeparator( Localizer.getString( headerText ),
            cc.xywh( 1, 1, 3, 1 ) );
        
        HTMLMultiLinePanel generalLines = new HTMLMultiLinePanel(
            Localizer.getString( contentText ) );
        builder.add( generalLines, cc.xy( 2, 3 ) );
    }
}