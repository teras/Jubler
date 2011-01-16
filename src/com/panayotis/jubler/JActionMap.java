/*
 *  JActionMap.java
 * 
 *  Created on: 20-Oct-2009 at 15:00:06
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * This file is part of Jubler.
 * 
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 * 
 * 
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Contributor(s):
 * 
 */
package com.panayotis.jubler;

import com.panayotis.jubler.events.menu.tool.VideoPreviewAction;
import com.panayotis.jubler.events.menu.edit.StyleBySelectionAction;
import com.panayotis.jubler.events.menu.edit.StepwiseAction;
import com.panayotis.jubler.events.menu.tool.ShiftTimeAction;
import com.panayotis.jubler.events.menu.file.RetrieveAction;
import com.panayotis.jubler.events.menu.edit.RemoveEmptyLineAction;
import com.panayotis.jubler.events.menu.edit.ByTimeAction;
import com.panayotis.jubler.events.menu.edit.ByLineNumberAction;
import com.panayotis.jubler.events.menu.tool.BeginningTimeVideoAction;
import com.panayotis.jubler.events.menu.toobar.PlayAudioAction;
import com.panayotis.jubler.events.menu.tool.CurrentTimeVideoAction;
import com.panayotis.jubler.events.menu.tool.EnablePreviewAction;
import com.panayotis.jubler.events.menu.toobar.SortAction;
import com.panayotis.jubler.events.menu.tool.RecodeTimeAction;
import com.panayotis.jubler.events.menu.tool.SpellAction;
import com.panayotis.jubler.events.menu.tool.RoundTimeAction;
import com.panayotis.jubler.events.menu.toobar.EnablePreviewToolBarAction;
import com.panayotis.jubler.events.menu.tool.SplitSubtitleAction;
import com.panayotis.jubler.events.menu.tool.SplitRecordAction;
import com.panayotis.jubler.events.menu.toobar.MaxWaveAction;
import com.panayotis.jubler.events.menu.toobar.HalfSizeZoomAction;
import com.panayotis.jubler.events.menu.toobar.AudioPreviewAction;
import com.panayotis.jubler.events.menu.popup.ShowTableColumnAction;
import com.panayotis.jubler.events.menu.edit.MarkSelectionAction;
import com.panayotis.jubler.events.menu.edit.PinkMarkAction;
import com.panayotis.jubler.events.menu.edit.NoneMarkAction;
import com.panayotis.jubler.events.menu.edit.CyanMarkAction;
import com.panayotis.jubler.events.menu.edit.YellowMarkAction;
import com.panayotis.jubler.events.menu.tool.TranslateAction;
import com.panayotis.jubler.events.menu.tool.TextBalancingOnTheWholeTableAction;
import com.panayotis.jubler.events.menu.tool.TextBalancingOnSelectionAction;
import com.panayotis.jubler.events.menu.tool.SynchronizeAction;
import com.panayotis.jubler.events.menu.edit.GoToSubtitleAction;
import com.panayotis.jubler.events.menu.tool.FixTimeAction;
import com.panayotis.jubler.events.menu.tool.JoinSubtitlesAction;
import com.panayotis.jubler.events.menu.edit.InsertSubEntryAction;
import com.panayotis.jubler.events.menu.edit.ReplaceGloballyAction;
import com.panayotis.jubler.events.menu.tool.ReparentAction;
import com.panayotis.jubler.events.menu.edit.DeleteSubtitleAction;
import com.panayotis.jubler.events.menu.edit.DeleteBySelectionAction;
import com.panayotis.jubler.events.menu.file.RevertFileAction;
import com.panayotis.jubler.events.menu.tool.OCRSelectedAction;
import com.panayotis.jubler.events.menu.tool.OCRAllAction;
import com.panayotis.jubler.events.app.AppCloseAction;
import com.panayotis.jubler.events.menu.edit.RedoEditAction;
import com.panayotis.jubler.events.menu.toobar.DropDownNumberOfLineAction;
import com.panayotis.jubler.events.menu.toobar.DropDownActionListAction;
import com.panayotis.jubler.events.menu.toobar.DoItAction;
import com.panayotis.jubler.events.menu.file.QuitAction;
import com.panayotis.jubler.events.menu.toobar.FileSaveToolBarButtonAction;
import com.panayotis.jubler.events.menu.file.FileCloseAction;
import com.panayotis.jubler.events.menu.help.FAQAction;
import com.panayotis.jubler.events.menu.edit.PasteSpecialAction;
import com.panayotis.jubler.events.menu.file.ChildAction;
import com.panayotis.jubler.events.menu.file.PreferenceSettingAction;
import com.panayotis.jubler.events.menu.file.InfoAction;
import com.panayotis.jubler.events.menu.edit.EditPasteAction;
import com.panayotis.jubler.events.menu.edit.EditCutAction;
import com.panayotis.jubler.events.menu.edit.EditUndoAction;
import com.panayotis.jubler.events.menu.edit.EditCopyAction;
import com.panayotis.jubler.events.menu.help.AboutHelpAction;
import com.panayotis.jubler.events.menu.file.FileSaveAsAction;
import com.panayotis.jubler.events.menu.file.FileNewAction;
import com.panayotis.jubler.events.menu.file.FileOpenAction;
import com.panayotis.jubler.events.menu.file.FileSaveAction;
import com.panayotis.jubler.events.*;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.events.menu.tool.duplication.RemoveBottomTopLineDuplication;
import com.panayotis.jubler.events.menu.tool.duplication.RemoveTimeDuplication;
import com.panayotis.jubler.events.menu.tool.duplication.RemoveTopLineDuplication;
import com.panayotis.jubler.events.menu.tool.duplication.SplitSONSubtitleAction;
import com.panayotis.jubler.events.menu.toobar.BalanceText;
import com.panayotis.jubler.events.menu.tool.EditTextCaseTranspose;
import com.panayotis.jubler.events.menu.toobar.InsertBlankLine;
import com.panayotis.jubler.events.menu.toobar.MoveRecord;
import com.panayotis.jubler.events.menu.toobar.MoveText;
import com.panayotis.jubler.events.menu.tool.ocr.OCRAction;
import com.panayotis.jubler.events.menu.tool.ocr.PackingImageFilesToTiffAction;
import com.panayotis.jubler.events.menu.tool.ocr.PackingImagesToTiffAction;
import com.panayotis.jubler.events.menu.file.AppendFromFile;
import com.panayotis.jubler.events.menu.file.ImportComponent;
import com.panayotis.jubler.events.menu.popup.ShowToolTipTextAction;
import com.panayotis.jubler.events.menu.tool.MergeRecords;
import com.panayotis.jubler.events.menu.tool.ViewHeader;
import com.panayotis.jubler.os.DEBUG;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.AbstractButton;
import javax.swing.DefaultButtonModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import static com.panayotis.jubler.subs.CommonDef.FILE_SEP;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JActionMap extends HashMap<Object, Object> {

    private Jubler jb = null;
    private AppCloseAction closeApp;
    private BalanceText balanceText;
    private CurrentTimeVideoAction videoPlayback;
    private CyanMarkAction cyanMark;
    private EditCopyAction copyAction;
    private EditCutAction cutAction;
    private EditPasteAction pasteAction;
    private EditUndoAction undoEdit;
    private FileNewAction fileNew;
    private FileOpenAction fileOpen;
    private GoToSubtitleAction gotoAction;
    private InfoAction info;
    private InsertBlankLine insertBlankLine;
    private InsertSubEntryAction insertSubEntry;
    private MergeRecords mergeRecords;
    private MoveRecord moveRecord;
    private MoveText moveText;
    private NoneMarkAction nonMark;
    private OCRAction ocrAction;
    private PinkMarkAction pinkMark;
    private RedoEditAction redoEdit;
    private ShowTableColumnAction showTableColumn;
    private ShowToolTipTextAction showToolTipTextAction;
    private YellowMarkAction yellowMark;

    public JActionMap(Jubler parent) {
        jb = parent;
        createlActionHandlers();
        addMap();
        addListeners();
    }//end public JActionMap(Jubler parent)

    private void addListeners() {
        jb.getDoItTB().addMouseListener(new MouseAdapter() {

            /**
             * {@inheritDoc}
             */
            public void mouseReleased(MouseEvent e) {
                int button_pressed = e.getButton();
                boolean is_right_mouse_button = (button_pressed == MouseEvent.BUTTON3);
                if (is_right_mouse_button) {
                    jb.fn.memoriseCurrentRow();
                }//end if (is_right_mouse_button)
            }
        });

    }

    private void addMap() {
        put(jb.getAboutHM(), new AboutHelpAction(jb));
        put(jb.getAfterIEM(), insertSubEntry);
        put(jb.getAppendFromFileFM(), new AppendFromFile(jb));
        put(jb.getAudioPreviewC(), new AudioPreviewAction(jb));
        put(jb.getBeforeIEM(), insertSubEntry);
        put(jb.getBeginningTTM(), new BeginningTimeVideoAction(jb));
        put(jb.getBottomGEM(), gotoAction);
        put(jb.getByLineNumberEM(), new ByLineNumberAction(jb));
        put(jb.getBySelectionDEM(), new DeleteBySelectionAction(jb));
        put(jb.getBySelectionMEM(), new MarkSelectionAction(jb));
        put(jb.getBySelectionSEM(), new StyleBySelectionAction(jb));
        put(jb.getByTimeGEM(), new ByTimeAction(jb));
        put(jb.getCaseTranspose(), new EditTextCaseTranspose(jb));
        put(jb.getChildNFM(), new ChildAction(jb));
        put(jb.getCloseFM(), new FileCloseAction(jb));
        put(jb.getCopyEM(), copyAction);
        put(jb.getCopyP(), copyAction);
        put(jb.getCopyTB(), copyAction);
        put(jb.getCurrentTTM(), videoPlayback);
        put(jb.getCutEM(), cutAction);
        put(jb.getCutP(), cutAction);
        put(jb.getCutTB(), cutAction);
        put(jb.getCyanMEM(), cyanMark);
        put(jb.getCyanMP(), cyanMark);
        put(jb.getDeleteP(), new DeleteSubtitleAction(jb));
        put(jb.getDoItTB(), new DoItAction(jb));
        put(jb.getDropDownActionList(), new DropDownActionListAction(jb));
        put(jb.getDropDownActionNumberOfLine(), new DropDownNumberOfLineAction(jb));
        put(jb.getEmptyLinesDEM(), new RemoveEmptyLineAction(jb));
        put(jb.getEnablePreviewC(), new EnablePreviewAction(jb));
        put(jb.getPreviewTB(), new EnablePreviewToolBarAction(jb));
        put(jb.getFAQHM(), new FAQAction(jb));
        put(jb.getFileNFM(), fileNew);
        put(jb.getFixTM(), new FixTimeAction(jb));
        put(jb.getGloballyREM(), new ReplaceGloballyAction(jb));
        put(jb.getHalfSizeC(), new HalfSizeZoomAction(jb));
        put(jb.getImportComponentFM(), new ImportComponent(jb));
        put(jb.getInfoFM(), info);
        put(jb.getInfoTB(), info);
        put(jb.getJoinRecordTM(), mergeRecords);
        put(jb.getJoinTM(), new JoinSubtitlesAction(jb));
        put(jb.getLoadTB(), fileOpen);
        put(jb.getMaxWaveC(), new MaxWaveAction(jb));
        put(jb.getNewTB(), fileNew);
        put(jb.getNextGEM(), gotoAction);
        put(jb.getNextPageGEM(), gotoAction);
        put(jb.getNoneMEM(), nonMark);
        put(jb.getNoneMP(), nonMark);
        put(jb.getOCRAll(), new OCRAllAction(jb));
        put(jb.getOCRSelected(), new OCRSelectedAction(jb));
        put(jb.getOpenFM(), fileOpen);
        put(jb.getPackingImageFilesToTiffFM(), new PackingImageFilesToTiffAction(jb));
        put(jb.getPackingImagesToTiffM(), new PackingImagesToTiffAction(jb));
        put(jb.getPasteEM(), pasteAction);
        put(jb.getPasteP(), pasteAction);
        put(jb.getPasteSpecialEM(), new PasteSpecialAction(jb));
        put(jb.getPasteTB(), pasteAction);
        put(jb.getPinkMEM(), pinkMark);
        put(jb.getPinkMP(), pinkMark);
        put(jb.getPlayAudioC(), new PlayAudioAction(jb));
        put(jb.getPlayVideoP(), videoPlayback);
        put(jb.getPrefsFM(), new PreferenceSettingAction(jb));
        put(jb.getPreviewTB(), new EnablePreviewToolBarAction(jb));
        put(jb.getPreviousGEM(), gotoAction);
        put(jb.getPreviousPageGEM(), gotoAction);
        put(jb.getQuitFM(), new QuitAction(jb));
        put(jb.getRecodeTM(), new RecodeTimeAction(jb));
        put(jb.getRedoEM(), redoEdit);
        put(jb.getRedoTB(), redoEdit);
        put(jb.getRemoveBottomTopLineDuplication(), new RemoveBottomTopLineDuplication(jb));
        put(jb.getRemoveTimeDuplication(), new RemoveTimeDuplication(jb));
        put(jb.getRemoveTopLineDuplication(), new RemoveTopLineDuplication(jb));
        put(jb.getReparentTM(), new ReparentAction(jb));
        put(jb.getRetrieveWFM(), new RetrieveAction(jb));
        put(jb.getRevertFM(), new RevertFileAction(jb));
        put(jb.getRoundTM(), new RoundTimeAction(jb));
        put(jb.getSaveAsFM(), new FileSaveAsAction(jb));
        put(jb.getSaveFM(), new FileSaveAction(jb));
        put(jb.getSaveTB(), new FileSaveToolBarButtonAction(jb));
        put(jb.getShiftTimeTM(), new ShiftTimeAction(jb));
        put(jb.getShowDurationP(), showTableColumn);
        put(jb.getShowEndP(), showTableColumn);
        put(jb.getShowLayerP(), showTableColumn);
        put(jb.getShowNumberP(), showTableColumn);
        put(jb.getShowStartP(), showTableColumn);
        put(jb.getShowStyleP(), showTableColumn);
        put(jb. getShowToolTipText(), this.showToolTipTextAction);
        put(jb.getSortTB(), new SortAction(jb));
        put(jb.getSpellTM(), new SpellAction(jb));
        put(jb.getSplitRecordTM(), new SplitRecordAction(jb));
        put(jb.getSplitSONSubtitleFile(), new SplitSONSubtitleAction(jb));
        put(jb.getSplitTM(), new SplitSubtitleAction(jb));
        put(jb.getStepwiseREM(), new StepwiseAction(jb));
        put(jb.getSynchronizeTM(), new SynchronizeAction(jb));
        put(jb.getTestTB(), videoPlayback);
        put(jb.getTextBalancingOnSelection(), new TextBalancingOnSelectionAction(jb));
        put(jb.getTextBalancingOnTheWholeTable(), new TextBalancingOnTheWholeTableAction(jb));
        put(jb.getTopGEM(), gotoAction);
        put(jb.getTranslateTM(), new TranslateAction(jb));
        put(jb.getUndoEM(), undoEdit);
        put(jb.getUndoTB(), undoEdit);
        put(jb.getVideoPreviewC(), new VideoPreviewAction(jb));
        put(jb.getViewHeaderTM(), new ViewHeader(jb));
        put(jb.getYellowMEM(), yellowMark);
        put(jb.getYellowMP(), yellowMark);

        Set keyset = keySet();
        Iterator it = keyset.iterator();
        while (it.hasNext()) {
            Object control = it.next();
            Object handler = get(control);

            boolean is_button = (control instanceof AbstractButton);
            boolean is_button_model = (control instanceof DefaultButtonModel);
            boolean is_combo_box = (control instanceof JComboBox);
            boolean is_jfile_chooser = (control instanceof JFileChooser);
            boolean is_jtext_field = (control instanceof JTextField);
            boolean is_action_listener =
                    is_button ||
                    is_button_model ||
                    is_combo_box ||
                    is_jfile_chooser ||
                    is_jtext_field;

            boolean valid =
                    (is_action_listener) &&
                    (handler instanceof ActionListener);
            if (valid) {
                ActionListener al = (ActionListener) handler;
                if (is_button) {
                    AbstractButton b = (AbstractButton) control;
                    b.addActionListener(al);
                } else if (is_button_model) {
                    DefaultButtonModel b = (DefaultButtonModel) control;
                    b.addActionListener(al);
                } else if (is_combo_box) {
                    JComboBox b = (JComboBox) control;
                    b.addActionListener(al);
                } else if (is_jfile_chooser) {
                    JFileChooser b = (JFileChooser) control;
                    b.addActionListener(al);
                } else if (is_jtext_field) {
                    JTextField b = (JTextField) control;
                    b.addActionListener(al);
                }//end if
            } else {
                System.out.println(_("Action listener not attached: " + control.getClass().getName()));
            }//end if (valid)
        }//end while (it.hasNext())
    }//end private void addMap()

    private void createlActionHandlers() {
        ocrAction = new OCRAction(jb);
        ocrAction.setLanguage("eng");
        String working_dir = FileCommunicator.getCurrentPath();
        File tesseract_path = new File(working_dir, "tesseract");
        String tesseract_path_as_string = tesseract_path.getAbsolutePath();
        ocrAction.setTessPath(tesseract_path_as_string + FILE_SEP);

        moveRecord =
                new MoveRecord(jb);
        moveText =
                new MoveText(jb);
        insertBlankLine =
                new InsertBlankLine(jb);
        balanceText =
                new BalanceText(jb);
        mergeRecords =
                new MergeRecords(jb);

        copyAction =
                new EditCopyAction(jb);
        cutAction =
                new EditCutAction(jb);
        pasteAction =
                new EditPasteAction(jb);
        undoEdit =
                new EditUndoAction(jb);
        redoEdit =
                new RedoEditAction(jb);
        insertSubEntry =
                new InsertSubEntryAction(jb);
        gotoAction =
                new GoToSubtitleAction(jb);
        closeApp =
                new AppCloseAction(jb);
        cyanMark =
                new CyanMarkAction(jb);
        pinkMark =
                new PinkMarkAction(jb);
        yellowMark =
                new YellowMarkAction(jb);
        nonMark =
                new NoneMarkAction(jb);
        info =
                new InfoAction(jb);
        fileNew =
                new FileNewAction(jb);
        fileOpen =
                new FileOpenAction(jb);
        videoPlayback =
                new CurrentTimeVideoAction(jb);
        showTableColumn =
                new ShowTableColumnAction(jb);
        showToolTipTextAction =
                new ShowToolTipTextAction(jb);        
    }//end private void createlActionHandlers()

    public boolean action(JComponent control) {
        boolean actioned = false;
        try {
            String name = control.getName();
            Object o = get(control);
            if (o instanceof ActionListener) {
                ActionEvent evt = new ActionEvent(control, ActionEvent.ACTION_PERFORMED, name);
                ActionListener al = (ActionListener) o;
                al.actionPerformed(evt);
                actioned = true;
            }//end if (al instanceof ActionListener){
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return actioned;
    }//end public void action(JComponent comp)

    /**
     * @return the moveRecord
     */
    public MoveRecord getMoveRecord() {
        return moveRecord;
    }

    /**
     * @return the moveText
     */
    public MoveText getMoveText() {
        return moveText;
    }

    /**
     * @return the insertBlankLine
     */
    public InsertBlankLine getInsertBlankLine() {
        return insertBlankLine;
    }

    /**
     * @return the balanceText
     */
    public BalanceText getBalanceText() {
        return balanceText;
    }

    /**
     * @return the mergeRecords
     */
    public MergeRecords getMergeRecords() {
        return mergeRecords;
    }

    /**
     * @return the insertSubEntry
     */
    public InsertSubEntryAction getInsertSubEntry() {
        return insertSubEntry;
    }

    /**
     * @return the gotoAction
     */
    public GoToSubtitleAction getGotoAction() {
        return gotoAction;
    }

    /**
     * @return the closeApp
     */
    public AppCloseAction getCloseApp() {
        return closeApp;
    }

    /**
     * @return the ocrAction
     */
    public OCRAction getOcrAction() {
        return ocrAction;
    }

    /**
     * @return the copyAction
     */
    public EditCopyAction getCopyAction() {
        return copyAction;
    }

    /**
     * @return the cutAction
     */
    public EditCutAction getCutAction() {
        return cutAction;
    }

    /**
     * @return the pasteAction
     */
    public EditPasteAction getPasteAction() {
        return pasteAction;
    }

    /**
     * @return the undoEdit
     */
    public EditUndoAction getUndoEdit() {
        return undoEdit;
    }
}//end public class JActionMap extends HashMap<String, Class>

class blankWrap extends MenuAction {

    public blankWrap(Jubler jb) {
        jublerParent = jb;
    }

    public void actionPerformed(ActionEvent evt) {
    }
}


