/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.tools.translate.Language;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Modeless satellite window (VLsub-style) bound to the {@link JubFrame} that opened it: one per frame,
 * closes with its owner. Search is free and cancellable; only the explicit button downloads, one at a
 * time, and every applied pick is an ordinary undoable operation on the owner's document.
 */
public class SubDownloadFrame extends JFrame {

    /** One window per owner frame. */
    private static final Map<JubFrame, SubDownloadFrame> OPEN = new IdentityHashMap<>();

    /** Remembered across windows and sessions. */
    private static final String PREF_LANGUAGE = "subdownload.language";
    private static final String PREF_PROVIDER = "subdownload.provider";

    private final JubFrame owner;
    private final SubtitleProvider[] providers = {
            new SubSourceProvider(), new OpenSubtitlesProvider(), new SubDLProvider()};

    private final JComboBox<String> providerBox = new JComboBox<>();
    private final JComboBox<Language> languageBox = new JComboBox<>(DownloadLanguages.list());
    private final JTextField queryField = new JTextField(24);
    private final JCheckBox hashCheck = new JCheckBox(__("Match by video hash"));
    private final JButton searchButton = new JButton(__("Search"));
    private final JButton configButton = new JButton(__("Configure…"));
    private final JButton downloadButton = new JButton(__("Download & preview"));
    private final CandidateTableModel model = new CandidateTableModel();
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");

    /** Session cache: a successfully downloaded candidate maps to its parsed temp file. Dies with us. */
    private final Map<Candidate, File> cache = new IdentityHashMap<>();

    private final WindowAdapter ownerWatcher;

    private SwingWorker<List<Candidate>, Void> searchWorker;
    private SubtitleProvider searchingProvider;
    private int searchSerial;
    private boolean downloading;

    /** For HASH_OPTIONAL providers only: the user's last explicit hash-vs-text choice, remembered per window. */
    private boolean hashPreferred = false;

    /** Open (or focus, if already open) the downloader for the given frame. */
    public static void openFor(JubFrame owner) {
        SubDownloadFrame existing = OPEN.get(owner);
        if (existing != null && existing.isDisplayable()) {
            existing.toFront();
            existing.requestFocus();
            return;
        }
        new SubDownloadFrame(owner).setVisible(true);
    }

    private SubDownloadFrame(JubFrame owner) {
        this.owner = owner;
        OPEN.put(owner, this);

        setTitle(__("Download subtitles") + " — " + documentName());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        prefillQuery();

        ownerWatcher = new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                dispose();
            }
        };
        owner.addWindowListener(ownerWatcher);

        pack();
        setSize(new Dimension(1180, 640));
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        for (SubtitleProvider p : providers)
            providerBox.addItem(p.getName());
        selectPersistedProvider();
        selectPersistedLanguage();

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        top.add(new JLabel(__("Provider")), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        top.add(providerBox, g);
        g.gridx = 2;
        g.fill = GridBagConstraints.NONE;
        top.add(configButton, g);

        g.gridx = 0;
        g.gridy = 1;
        top.add(new JLabel(__("Language")), g);
        g.gridx = 1;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        top.add(languageBox, g);
        g.gridwidth = 1;

        g.gridx = 1;
        g.gridy = 2;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        top.add(hashCheck, g);
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy = 3;
        g.fill = GridBagConstraints.NONE;
        top.add(new JLabel(__("Search")), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        top.add(queryField, g);
        g.gridx = 2;
        g.fill = GridBagConstraints.NONE;
        top.add(searchButton, g);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateDownloadEnabled());
        // The icon headers carry no text, so surface the full localized column name as a header tooltip.
        table.setTableHeader(new JTableHeader(table.getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int viewCol = columnAtPoint(e.getPoint());
                if (viewCol < 0)
                    return null;
                int modelCol = table.convertColumnIndexToModel(viewCol);
                return modelCol >= 1 && modelCol <= 3 ? model.getColumnName(modelCol) : null;
            }
        });
        configureColumns();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(900, 420));

        // The status/error line must never dictate the window width: give it a zero preferred/minimum
        // width so pack() ignores its (possibly very long) message. In its BorderLayout.CENTER cell it
        // still stretches to fill the leftover row width and clips overflow with an ellipsis; the full
        // text stays reachable via the tooltip (see showInfo/showError).
        int statusHeight = status.getPreferredSize().height;
        status.setMinimumSize(new Dimension(0, statusHeight));
        status.setPreferredSize(new Dimension(0, statusHeight));
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(downloadButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> startSearch());
        queryField.addActionListener(e -> startSearch());
        configButton.addActionListener(e -> currentProvider().configure(this));
        downloadButton.addActionListener(e -> startDownload());
        providerBox.addActionListener(e -> {
            updateConfigEnabled();
            updateHashControls();
            JublerPrefs.set(PREF_PROVIDER, currentProvider().getName());
        });
        languageBox.addActionListener(e -> persistLanguage());
        hashCheck.addActionListener(e -> {
            if (currentProvider().hashSupport() == SubtitleProvider.HashSupport.HASH_OPTIONAL)
                hashPreferred = hashCheck.isSelected();
            updateHashControls();
        });
        // Re-evaluate the hash controls when the window regains focus: the user may have opened or closed a
        // video in the owner frame meanwhile, which flips whether hashing is available.
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                updateHashControls();
            }
        });

        getRootPane().setDefaultButton(searchButton);
        updateConfigEnabled();
        updateDownloadEnabled();
        updateHashControls();
    }

    /** Keyless providers need no key: hide the Configure control so it never prompts. */
    private void updateConfigEnabled() {
        configButton.setVisible(currentProvider().needsConfiguration());
    }

    private SubtitleProvider currentProvider() {
        return providers[Math.max(0, providerBox.getSelectedIndex())];
    }

    private VideoFile currentVideo() {
        MediaFile m = owner.getMediaFile();
        return m == null ? null : m.getVideoFile();
    }

    private boolean hasVideo() {
        VideoFile v = currentVideo();
        return v != null && v.isFile();
    }

    /**
     * Sync the "Match by video hash" checkbox and the query field to the selected provider and whether a
     * video is open. TEXT_ONLY providers force the box off; the HASH_OPTIONAL provider offers it only when a
     * video is open, honouring the user's remembered choice. When hashing is on, the free-text query is
     * irrelevant, so the query field is disabled.
     */
    private void updateHashControls() {
        SubtitleProvider.HashSupport support = currentProvider().hashSupport();
        boolean hasVideo = hasVideo();
        switch (support) {
            case HASH_OPTIONAL:
                hashCheck.setEnabled(hasVideo);
                hashCheck.setSelected(hasVideo && hashPreferred);
                break;
            case TEXT_ONLY:
                hashCheck.setEnabled(false);
                hashCheck.setSelected(false);
                break;
        }
        queryField.setEnabled(!hashCheck.isSelected());
    }

    private void updateDownloadEnabled() {
        downloadButton.setEnabled(!downloading && table.getSelectedRow() >= 0);
    }

    /**
     * Column layout: Release (Έκδοση) holds long release names, so it is the only stretchy column — it
     * absorbs every extra pixel when the window widens. The other three are narrow and fixed (max == pref):
     * with zero slack they never grow, so all the slack falls to Release under the default resize policy.
     * Their headers are icons (see {@link #installIconHeaders()}), so they can be narrow.
     */
    private void configureColumns() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        javax.swing.table.TableColumnModel cols = table.getColumnModel();
        cols.getColumn(0).setPreferredWidth(420);       // Release / Έκδοση — stretches
        fixColumnWidth(cols.getColumn(1), 90);          // Language / Γλώσσα (globe icon) — fits a language name
        fixColumnWidth(cols.getColumn(2), 78);          // Downloads / Λήψεις (download icon) — fits multi-digit counts
        fixColumnWidth(cols.getColumn(3), 78);          // Rating / Βαθμολογία (star icon)
        centerColumn(cols.getColumn(2));                // numbers sit under the centred icon
        centerColumn(cols.getColumn(3));
        installIconHeaders();
    }

    /**
     * The three narrow columns render an SVG glyph instead of their (truncation-prone Greek) header text:
     * globe for Language, download arrow for Downloads, star for Rating. The full localized name stays
     * reachable through the header tooltip (see the JTableHeader override in {@link #buildUI()}).
     */
    private void installIconHeaders() {
        TableCellRenderer base = table.getTableHeader().getDefaultRenderer();
        setIconHeader(1, "flag-global", base);
        setIconHeader(2, "download", base);
        setIconHeader(3, "star", base);
    }

    private void setIconHeader(int column, String iconName, TableCellRenderer base) {
        Icon icon = Theme.loadIcon(iconName);
        table.getColumnModel().getColumn(column).setHeaderRenderer(new IconHeaderRenderer(base, icon));
    }

    /** Pin a column to a fixed width so it neither grows nor donates the row's slack. */
    private static void fixColumnWidth(javax.swing.table.TableColumn column, int width) {
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

    /** Centre a numeric column's cell text so it sits under its centred icon header. */
    private static void centerColumn(javax.swing.table.TableColumn column) {
        javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        column.setCellRenderer(renderer);
    }

    /**
     * Draws a column header as a centred icon with no text, delegating to the L&F's default header
     * renderer for background/border/state so it stays visually consistent with the text headers.
     */
    private static class IconHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer base;
        private final Icon icon;

        IconHeaderRenderer(TableCellRenderer base, Icon icon) {
            this.base = base;
            this.icon = icon;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setText(null);
                label.setIcon(icon);
                label.setHorizontalAlignment(SwingConstants.CENTER);
            }
            return c;
        }
    }

    /* ---- Search ---- */

    private void startSearch() {
        updateHashControls();
        boolean useHash = hashCheck.isSelected();
        VideoFile video = currentVideo();
        if (useHash && (video == null || !video.isFile())) {
            showError(__("Open a video first to match by hash."));
            return;
        }
        String query = useHash ? "" : sanitizeQuery(queryField.getText());
        if (!useHash && query.isEmpty()) {
            showError(__("Enter something to search for."));
            return;
        }
        SubtitleProvider provider = currentProvider();
        String notReady = provider.ensureReady(this);
        if (notReady != null) {
            showError(notReady);
            return;
        }

        // Supersede any in-flight search: disconnect it and bump the serial so its late result is ignored.
        if (searchWorker != null && !searchWorker.isDone()) {
            if (searchingProvider != null)
                searchingProvider.cancelSearch();
            searchWorker.cancel(true);
        }
        final int mySerial = ++searchSerial;
        final SearchRequest req = new SearchRequest(query, selectedLanguageCode(), useHash, video);

        searchingProvider = provider;
        searchButton.setEnabled(false);
        showInfo(__("Searching…"));

        searchWorker = new SwingWorker<List<Candidate>, Void>() {
            @Override
            protected List<Candidate> doInBackground() throws Exception {
                return provider.search(req);
            }

            @Override
            protected void done() {
                if (mySerial != searchSerial)
                    return; // a newer search took over; discard this stale response silently
                searchButton.setEnabled(true);
                try {
                    List<Candidate> results = get();
                    model.setCandidates(results);
                    updateDownloadEnabled();
                    showInfo(results.isEmpty() ? __("No subtitles found.")
                            : __("{0} results.", results.size()));
                } catch (java.util.concurrent.CancellationException ignored) {
                    // superseded
                } catch (Exception ex) {
                    reportFailure(ex);
                }
            }
        };
        searchWorker.execute();
    }

    /* ---- Download ---- */

    private void startDownload() {
        if (downloading)
            return;
        int row = table.getSelectedRow();
        if (row < 0) {
            showError(__("Select a subtitle first."));
            return;
        }
        final Candidate candidate = model.get(table.convertRowIndexToModel(row));
        if (candidate == null) {
            showError(__("Select a subtitle first."));
            return;
        }

        // Re-picking a cached candidate re-applies instantly with no network.
        File cached = cache.get(candidate);
        if (cached != null) {
            applyAndReport(candidate, cached, true, null);
            return;
        }

        String notReady = candidate.getProvider().ensureReady(this);
        if (notReady != null) {
            showError(notReady);
            return;
        }

        downloading = true;
        updateDownloadEnabled();
        showInfo(__("Downloading…"));

        final java.util.concurrent.atomic.AtomicReference<String> contentType = new java.util.concurrent.atomic.AtomicReference<>();
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                DownloadData dl = candidate.getProvider().download(candidate);
                contentType.set(dl.contentType);
                return DownloadApply.toTempFile(dl.data, candidate.getFileHint());
            }

            @Override
            protected void done() {
                downloading = false;
                try {
                    File file = get();
                    applyAndReport(candidate, file, false, contentType.get());
                } catch (Exception ex) {
                    reportFailure(ex);
                }
                updateDownloadEnabled();
            }
        }.execute();
    }

    /** Apply an already-downloaded file to the owner document (EDT), updating row state and status. */
    private void applyAndReport(Candidate candidate, File file, boolean fromCache, String contentType) {
        String error = DownloadApply.applyFile(owner, file, candidate.getReleaseName(),
                candidate.getProvider().getName(), contentType);
        if (error == null) {
            cache.put(candidate, file);
            showInfo(fromCache ? __("Applied from cache.") : __("Downloaded and applied."));
        } else {
            showError(error);
        }
    }

    /** Show a background failure as a plain factual message (providers already word their own errors). */
    private void reportFailure(Exception ex) {
        Throwable cause = ex instanceof java.util.concurrent.ExecutionException ? ex.getCause() : ex;
        // Expected provider errors carry a clean message (logged by showError below); dumping their stack
        // trace too is redundant noise. Keep full traces only for unexpected/programming errors.
        if (cause != null && !(cause instanceof ProviderException))
            DEBUG.debug(cause);
        if (cause instanceof ProviderException) {
            showError(cause.getMessage());
            return;
        }
        showError(cause == null ? __("Operation failed.") : String.valueOf(cause.getMessage()));
    }

    /* ---- Helpers ---- */

    private void showInfo(String text) {
        status.setForeground(UIManager.getColor("Label.foreground"));
        setStatusText(text);
    }

    private void showError(String text) {
        status.setForeground(new Color(0xB0, 0x30, 0x30));
        setStatusText(text);
        DEBUG.debug(text); // mirror every user-facing error into Jubler's log/console
    }

    /** Set the status line, mirroring the full message into the tooltip so a clipped line stays readable. */
    private void setStatusText(String text) {
        status.setText(text);
        status.setToolTipText(text == null || text.trim().isEmpty() ? null : text);
    }

    private String selectedLanguageCode() {
        Object sel = languageBox.getSelectedItem();
        return sel instanceof Language ? ((Language) sel).id : "";
    }

    /** Preselect the last-used language (remembered across sessions), falling back to English. */
    private void selectPersistedLanguage() {
        String remembered = JublerPrefs.getString(PREF_LANGUAGE, "en");
        if (selectLanguageById(remembered))
            return;
        selectLanguageById("en");
    }

    private boolean selectLanguageById(String id) {
        for (int i = 0; i < languageBox.getItemCount(); i++)
            if (languageBox.getItemAt(i).id.equals(id)) {
                languageBox.setSelectedIndex(i);
                return true;
            }
        return false;
    }

    private void persistLanguage() {
        Object sel = languageBox.getSelectedItem();
        if (sel instanceof Language)
            JublerPrefs.set(PREF_LANGUAGE, ((Language) sel).id);
    }

    /** Preselect the last-used provider (remembered across sessions). */
    private void selectPersistedProvider() {
        String remembered = JublerPrefs.getString(PREF_PROVIDER, "");
        if (remembered.isEmpty())
            return;
        for (int i = 0; i < providers.length; i++)
            if (providers[i].getName().equals(remembered)) {
                providerBox.setSelectedIndex(i);
                return;
            }
    }

    private void prefillQuery() {
        MediaFile media = owner.getMediaFile();
        VideoFile video = media == null ? null : media.getVideoFile();
        if (video != null) {
            String name = cleanName(video.getName());
            if (!name.isEmpty())
                queryField.setText(name);
        }
    }

    private String documentName() {
        MediaFile media = owner.getMediaFile();
        VideoFile video = media == null ? null : media.getVideoFile();
        if (video != null && !video.getName().isEmpty())
            return video.getName();
        File sub = owner.getSubtitles() == null ? null : owner.getSubtitles().getSubFile().getSaveFile();
        if (sub != null && !sub.getName().isEmpty())
            return sub.getName();
        return __("Untitled");
    }

    /**
     * A search query is free text: never reject it for its characters (dots, brackets, apostrophes, '&',
     * accented/Greek/CJK letters, digits are all legitimate). We only drop control characters and collapse
     * whitespace, silently. Correct transport is the providers' job via URL-encoding.
     */
    private static String sanitizeQuery(String raw) {
        if (raw == null)
            return "";
        return raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
    }

    private static String cleanName(String fileName) {
        if (fileName == null)
            return "";
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        base = base.replaceAll("[._]+", " ").replaceAll("\\s+", " ").trim();
        return base;
    }

    @Override
    public void dispose() {
        OPEN.remove(owner, this);
        owner.removeWindowListener(ownerWatcher);
        if (searchWorker != null && !searchWorker.isDone()) {
            if (searchingProvider != null)
                searchingProvider.cancelSearch();
            searchWorker.cancel(true);
        }
        super.dispose();
    }
}
