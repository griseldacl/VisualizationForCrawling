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
 *  $Id: DlgResultFind.java,v 1.10 2005/10/03 00:18:23 gregork Exp $
 */
package phex.dialogues;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.*;

import phex.common.ServiceManager;
import phex.gui.common.GUIUtils;
import phex.gui.common.MainFrame;
import phex.interfaces.IFind;

//TODO2 intergrate improved find in tables... 
public class DlgResultFind extends JDialog implements KeyListener
{
    private JTextField mTextValue;
    private JCheckBox			mMatchCase;
    private JRadioButton		mFindUp;
    private JRadioButton		mFindDown;
    private IFind				mSearchCallback;



    public DlgResultFind(MainFrame frame, IFind searchCallback)
    {
        // Call base class to set up modal dialog box.
        super(frame, true); // resolve; modaless?
        mSearchCallback = searchCallback;
        setTitle("Find");

        String	oldText = "";

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        // Set up panel and borders.
        JPanel		outerPanel = new JPanel();
        outerPanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(12, 12, 12, 12),
                    BorderFactory.createEtchedBorder()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        outerPanel.setLayout(new BorderLayout());

        JPanel		centerPanel = new JPanel(new GridLayout(3, 1, 0, 0));

        // Add label and input field.
        JLabel		labelFind = new JLabel("Find Text:");
        centerPanel.add(labelFind);
        mTextValue = new JTextField(oldText, 20);
        mTextValue.setText(ServiceManager.sCfg.mFindText);
        labelFind.setDisplayedMnemonic('T');
        labelFind.setLabelFor(mTextValue);
        centerPanel.add(mTextValue);

        JPanel		optionPanel = new JPanel(new GridLayout(1, 3, 0, 8));
        mMatchCase = new JCheckBox("Match case      ", ServiceManager.sCfg.mFindMatchCase);
        mMatchCase.setMnemonic('C');
        mFindUp = new JRadioButton("Find Up", !ServiceManager.sCfg.mFindDown);
        mFindUp.setMnemonic('U');
        mFindDown = new JRadioButton("Find Down", ServiceManager.sCfg.mFindDown);
        mFindDown.setMnemonic('N');
        ButtonGroup	group = new ButtonGroup();
        group.add(mFindUp);
        group.add(mFindDown);
        optionPanel.add(mMatchCase);
        optionPanel.add(mFindUp);
        optionPanel.add(mFindDown);

        centerPanel.add(optionPanel);

        // Set up the Find and Done buttons.
        JPanel		rightPanel = new JPanel(new BorderLayout());
        JPanel		buttonOuterPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JPanel		buttonPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        JButton		find = new JButton("Find Next");
        JButton		done = new JButton("Done");
        find.setMnemonic('F');
        done.setMnemonic('D');
        buttonPanel.add(find);
        buttonPanel.add(done);
        buttonOuterPanel.add(buttonPanel);
        rightPanel.add(BorderLayout.CENTER, new JLabel("    "));
        rightPanel.add(BorderLayout.EAST, buttonOuterPanel);

        outerPanel.add(BorderLayout.CENTER, centerPanel);
        outerPanel.add(BorderLayout.EAST, rightPanel);

        getContentPane().add(BorderLayout.CENTER, outerPanel);

        // Listener for the find and done buttons.
        ActionListener	actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                if (event.getActionCommand().equals("Done"))
                {
                    doDone();
                }
                else if (event.getActionCommand().equals("Find Next"))
                {
                    doFind();
                }
            }
        };

        find.addActionListener(actionListener);
        done.addActionListener(actionListener);

        addKeyListener(this);

        pack();

        GUIUtils.centerWindowOnScreen(this);
    }


    private void doDone()
    {
        setVisible(false);
        dispose();
    }


    private void doFind()
    {
        ServiceManager.sCfg.mFindMatchCase = mMatchCase.isSelected();
        ServiceManager.sCfg.mFindDown= mFindDown.isSelected();
        ServiceManager.sCfg.mFindText = mTextValue.getText();

        mSearchCallback.findInResult(mMatchCase.isSelected(), mFindDown.isSelected(), mTextValue.getText());
    }


    // KeyListener implementation
    public void keyPressed(KeyEvent e)
    {
    }


    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
            doDone();
        }
        else if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            doFind();
        }
    }


    public void keyTyped(KeyEvent e)
    {
    }



}


