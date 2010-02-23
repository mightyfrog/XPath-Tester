package org.mightyfrog.util.xpathtester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;


/**
 *
 */
public class XPathTester extends JFrame {
    //
    private final XPathTree TREE = new XPathTree();

    //
    private final JLabel EXP_LABEL = new JLabel("XPath:");
    private final XPathField EXP_FIELD = new XPathField();

    //
    private final JTextField STATUS_FIELD = new JTextField() {
            {
                setEditable(false);
                setFocusable(false);
                setBorder(null);
                setDragEnabled(true);
            }
        };

    // file menu
    private JMenu fileMenu = null;
    private JMenuItem openMI = null;
    private JMenuItem urlMI = null;
    private JMenuItem exitMI = null;

    // plugin menu
    private JMenu pluginMenu = null;

    // option menu
    private JMenu optionMenu = null;
    private JMenuItem encodingMI = null;
    private JCheckBoxMenuItem ignoreEmptyTextNodeMI = null;
    private JCheckBoxMenuItem nsAwareMI = null;
    private JMenuItem clearCacheMI = null;

    // help menu
    private JMenu helpMenu = null;
    private JMenuItem wikiMI = null;
    private JMenuItem aboutMI = null;

    //
    private JFileChooser fileChooser = null;

    //
    private Map<String, Class> pluginMap = new HashMap<String, Class>();

    //
    private String currentUrl = null;

    //
    private final JTabbedPane TABBED_PANE =
        new JTabbedPane(JTabbedPane.BOTTOM);

    //
    private final JTextArea TA = new JTextArea();

    /**
     *
     */
    public XPathTester() {
        loadPlugins();

        JOptionPane.setRootFrame(this);
        setTitle(I18N.get("frame.title"));
        setIconImage(getIcon("icon.png").getImage());

        ((JPanel) getContentPane()).
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        add(createNorthPanel(), BorderLayout.NORTH);
        TABBED_PANE.addTab(I18N.get("tab.tree"), createCenterPanel());
        TABBED_PANE.addTab(I18N.get("tab.result"), new JScrollPane(TA));
        add(TABBED_PANE);
        add(createSouthPanel(), BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        setStatus(I18N.get("status.ok"));
    }

    /**
     *
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            javax.swing.JComponent.setDefaultLocale(I18N.getLocale());
        } catch (Exception e) {
            // ClassNotFoundException, InstantiationException
            // IllegalAccessException
            // javax.swing.UnsupportedLookAndFeelException
        }

        EventQueue.invokeLater(new Runnable() {
                /** */
                @Override
                public void run() {
                    new XPathTester();
                }
            });
    }

    //
    //
    //

    /**
     *
     * @param running
     */
    void toggleCursor(boolean running) {
        Component gp = getGlassPane();
        if (running) {
            gp.addMouseListener(new MouseAdapter() {
                    /** */
                    @Override
                    public void mousePressed(MouseEvent evt) {
                        evt.consume();
                    }
                });
            gp.setVisible(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            MouseListener[] ml = gp.getMouseListeners();
            for (MouseListener l : ml) {
                gp.removeMouseListener(l);
            }
            gp.setVisible(false);
            setCursor(null);
        }
    }

    /**
     *
     * @param in
     */
    void load(InputStream in) {
        try {
            TREE.load(in);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                          e.getLocalizedMessage(),
                                          e.getClass().getName(),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        EXP_FIELD.requestFocusInWindow();
        setStatus(I18N.get("status.ok"));
    }

    /**
     *
     * @param text
     */
    void setStatus(String text) {
        setStatus(text, false);
    }

    /**
     *
     * @param text
     */
    void setMessage(String text) {
        TA.append(text);
    }

    /**
     *
     * @param text
     * @param error
     */
    void setStatus(String text, boolean error) {
        if (error) {
            STATUS_FIELD.setForeground(Color.RED);
        } else {
            STATUS_FIELD.setForeground(SystemColor.textText);
        }
        STATUS_FIELD.setText(null);
        STATUS_FIELD.setText(text);
    }

    //
    //
    //

    /**
     * Finds and returns instances of org.mightyfrog.util.xpathtester.Plugin
     * implementations.
     *
     * @param file the jar file that contains the plugin class
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    private void findPluginClass(File file) throws IOException {
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(file));
            JarEntry je = null;
            while ((je = jis.getNextJarEntry()) != null) {
                String name = je.getName();
                if (name.endsWith(".class")) {
                    name = name.replaceAll("/", ".");
                    name = name.substring(0, name.lastIndexOf(".class"));
                    try {
                        Class c = Class.forName(name);
                        if (Plugin.class.isAssignableFrom(c)) {
                            Constructor cs = c.getDeclaredConstructor();
                            Method m = c.getMethod("getName");
                            String pName = (String) m.invoke(cs.newInstance());
                            this.pluginMap.put(pName, c);
                        }
                    } catch (NoClassDefFoundError e) {
                        continue;
                    } catch (Exception e) {
                        // NoSuchMethodException, InstantiationException
                        // IllegalAccessException, ClassNotFoundException
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     *
     */
    private JPanel createNorthPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 0));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 5;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(3, 0, 3, 3);

        Action evalAction = new AbstractAction() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (!TREE.cancelStoreTask()) {
                        setStatus("Another task is running...");
                        return;
                    }
                    String exp = EXP_FIELD.getText().trim();
                    if (exp.isEmpty()) {
                        return;
                    }
                    try {
                        toggleCursor(true);
                        int nodeCount = TREE.evaluate(exp);
                        if (nodeCount != -1) {
                            setStatus(I18N.get("status.0", nodeCount));
                        } else {
                            try {
                                setStatus(TREE.evaluateFunction(exp));
                            } catch (Exception e) {
                                if (TREE.getDocument() == null) {
                                    setStatus(I18N.get("status.1"), true);
                                } else {
                                    if (e.getCause() != null) {
                                        String msg =
                                            e.getCause().getLocalizedMessage();
                                        setStatus(msg, true);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        try {
                            Object obj = TREE.evaluateFunction(exp);
                            setMessage(I18N.get("msg.function", obj));
                            setStatus(obj.toString());
                        } catch (Exception ex) {
                            if (e.getCause() != null) {
                                String msg = e.getCause().getLocalizedMessage();
                                setStatus(msg, true);
                            }
                        }
                    } finally {
                        toggleCursor(false);
                    }
                }
            };
        EXP_FIELD.getActionMap().put("eval", evalAction);
        EXP_FIELD.getInputMap().put(KeyStroke.getKeyStroke("pressed ENTER"),
                                    "eval");
        EXP_LABEL.setLabelFor(EXP_FIELD);
        EXP_LABEL.setDisplayedMnemonic('X');
        panel.add(EXP_LABEL, gbc);
        gbc.weightx = 1.0;
        panel.add(EXP_FIELD, gbc);
        gbc.weightx = 0.0;
        IconButton execButton = new IconButton(getIcon("exec.png"),
                                               I18N.get("button.evaluate"));
        execButton.addActionListener(evalAction);
        panel.add(execButton, gbc);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        IconButton prevButton = new IconButton(getIcon("prev.png"),
                                               I18N.get("button.prev"));
        prevButton.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    TREE.findPrevious();
                }
            });
        panel.add(prevButton, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        IconButton nextButton = new IconButton(getIcon("next.png"),
                                               I18N.get("button.next"));
        nextButton.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    TREE.findNext();
                }
            });
        panel.add(nextButton, gbc);

        return panel;
    }

    /**
     *
     * @param fileName
     */
    private ImageIcon getIcon(String fileName) {
        return new ImageIcon(XPathTester.class.getResource(fileName));
    }

    /**
     *
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        panel.add(new JScrollPane(TREE));

        return panel;
    }

    /**
     *
     */
    private JPanel createSouthPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(STATUS_FIELD);

        return panel;
    }

    /**
     *
     */
    private File openFile() {
        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser(new File("."));
       }
        int option = fileChooser.showOpenDialog(this);
        File file = null;
        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
        }

        return file;
    }

    /**
     *
     */
    private File saveFile() {
        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser();
       }
        int option = fileChooser.showSaveDialog(this);
        File file = null;
        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
        }

        return file;
    }

    /**
     *
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(createFileMenu());
        menuBar.add(createOptionMenu());
        menuBar.add(createHelpMenu());

        return menuBar;
    }

    /**
     *
     */
    private JMenu createFileMenu() {
        this.fileMenu = new JMenu(I18N.get("menu.file"));
        this.openMI = new JMenuItem(I18N.get("menuitem.open.file"));
        this.urlMI = new JMenuItem(I18N.get("menuitem.open.url"));
        this.exitMI = new JMenuItem(I18N.get("menuitem.exit"));
        this.openMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    File file = openFile();
                    if (file == null) {
                        return;
                    }
                    InputStream in = null;
                    try {
                        in = new FileInputStream(file);
                        load(in);
                    } catch (IOException e) {
                        return;
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        this.urlMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    openURL();
                }
            });
        this.exitMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    System.exit(0);
                }
            });

        this.fileMenu.setMnemonic('F');
        this.openMI.setMnemonic('O');
        this.urlMI.setMnemonic('U');
        this.exitMI.setMnemonic('X');

        this.openMI.setAccelerator(KeyStroke.getKeyStroke("ctrl pressed O"));
        this.urlMI.setAccelerator(KeyStroke.getKeyStroke("ctrl pressed U"));

        this.fileMenu.add(this.openMI);
        this.fileMenu.add(this.urlMI);
        this.fileMenu.addSeparator();
        this.fileMenu.add(this.exitMI);

        return this.fileMenu;
    }

    /**
     *
     */
    private String getCurrentURL() {
        return this.currentUrl == null ? "http://" : this.currentUrl;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    private void openURL() {
        String s = JOptionPane.showInputDialog(JOptionPane.getRootFrame(),
                                               I18N.get("dialog.1"),
                                               getCurrentURL());
        if (s != null && !s.trim().isEmpty() &&  s.trim().length() < 256) {
            this.currentUrl = s;
            URL url = null;
            try {
                url = cacheData(new URL(s));
                String pluginName = getPluginName();
                if (!pluginName.equals(I18N.get("menuitem.none"))) {
                    Class plugin = this.pluginMap.get(pluginName);
                    if (plugin != null) {
                        Constructor c = plugin.getDeclaredConstructor();
                        Plugin p = (Plugin) c.newInstance();
                        Method m = plugin.getMethod("setCharset", Charset.class);
                        m.invoke(p, Charset.forName(TREE.getEncoding()));
                        m = plugin.getMethod("getDocument", URL.class);
                        Document doc = (Document) m.invoke(p, url);
                        TREE.load(doc);
                    } else {
                        throw new ClassNotFoundException(I18N.get("dialog.2"));
                    }
                } else {
                    load(url.openStream());
                }
            } catch (Exception e) { // MalformedURLException, IOException
                e.printStackTrace();
                String msg = e.getLocalizedMessage();
                if (msg == null) {
                    Throwable t  = e.getCause();
                    while (t != null) {
                        msg = t.getLocalizedMessage();
                        if (msg != null) {
                            break;
                        }
                        t = t.getCause();
                    }
                }
                if (msg == null || msg.trim().length() == 0) {
                    msg = I18N.get("dialog.3");
                }
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                              msg,
                                              e.getClass().getName(),
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     *
     * @param url
     */
    private URL cacheData(URL url) {
        File cacheDir = new File("cache");
        if (!cacheDir.exists()) {
            if (cacheDir.mkdir()) {
                return url;
            }
        }
        FileInputStream fis = null;
        File cache = new File(cacheDir, "" + url.toString().hashCode());
        if (!cache.exists()) {
            BufferedInputStream in = null;
            FileOutputStream out = null;
            int n = 0;
            byte[] b = new byte[2048];
            try {
                out = new FileOutputStream(cache);
                in = new BufferedInputStream(url.openStream());
                while ((n = in.read(b)) != -1) {
                    out.write(b, 0, n);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        try {
            url = cache.toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     *
     */
    private String getPluginName() {
        String pluginName = null;
        java.awt.Component[] comps = this.pluginMenu.getMenuComponents();
        for (java.awt.Component c : comps) {
            JMenuItem mi = (JMenuItem) c;
            if (mi.isSelected()) {
                pluginName = mi.getActionCommand();
            }
        }

        return pluginName;
    }

    /**
     *
     */
    private JMenu createPluginMenu() {
        this.pluginMenu = new JMenu(I18N.get("menu.plugins"));
        ButtonGroup bg = new ButtonGroup();
        JRadioButtonMenuItem noneMI =
            new JRadioButtonMenuItem(I18N.get("menuitem.none"));
        noneMI.setSelected(true);
        bg.add(noneMI);
        this.pluginMenu.add(noneMI);

        for (String name : this.pluginMap.keySet()) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(name);
            mi.setToolTipText(this.pluginMap.get(name).getName());
            this.pluginMenu.add(mi);
            bg.add(mi);
        }

        return this.pluginMenu;
    }

    /**
     *
     */
    private JMenu createOptionMenu() {
        this.optionMenu = new JMenu(I18N.get("menu.options"));
        this.encodingMI = new JMenuItem(I18N.get("menuitem.set.encoding"));
        this.ignoreEmptyTextNodeMI =
            new JCheckBoxMenuItem(I18N.get("menuitem.hide.empty.text.nodes"));
        this.nsAwareMI = new JCheckBoxMenuItem(I18N.get("menuitem.ns.aware"));
        this.clearCacheMI = new JMenuItem(I18N.get("menuitem.clear.cache"));
        this.encodingMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    String encoding =
                        JOptionPane.showInputDialog(JOptionPane.getRootFrame(),
                                                    I18N.get("dialog.0"),
                                                    TREE.getEncoding());
                    if (encoding != null) {
                        TREE.setEncoding(encoding);
                        showReloadRequiredDialog();
                    }
                }
            });
        this.ignoreEmptyTextNodeMI.addItemListener(new ItemListener() {
                /** */
                @Override
                public void itemStateChanged(ItemEvent evt) {
                    JCheckBoxMenuItem mi = (JCheckBoxMenuItem) evt.getSource();
                    TREE.setIgnoreEOL(mi.isSelected());
                    showReloadRequiredDialog();
                }
            });
        //this.nsAwareMI.setSelected(true);
        this.nsAwareMI.addItemListener(new ItemListener() {
                /** */
                @Override
                public void itemStateChanged(ItemEvent evt) {
                    JCheckBoxMenuItem mi = (JCheckBoxMenuItem) evt.getSource();
                    TREE.setNamespaceAware(mi.isSelected());
                    showReloadRequiredDialog();
                }
            });
        this.clearCacheMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    File cacheDir = new File("cache");
                    if (cacheDir.exists()) {
                        int option = showYesNoDialog(I18N.get("dialog.5"));
                        if (option == JOptionPane.OK_OPTION) {
                            File[] caches = cacheDir.listFiles();
                            for (File file : caches) {
                                if (file.delete()) {
                                    file.deleteOnExit();
                                }
                            }
                            if (cacheDir.delete()) {
                                cacheDir.deleteOnExit();
                            }
                        }
                    }
                }
            });

        this.ignoreEmptyTextNodeMI.setSelected(true);

        this.optionMenu.add(this.encodingMI);
        this.optionMenu.add(this.ignoreEmptyTextNodeMI);
        this.optionMenu.add(this.nsAwareMI);
        this.optionMenu.addSeparator();
        this.optionMenu.add(createPluginMenu());
        this.optionMenu.addSeparator();
        this.optionMenu.add(this.clearCacheMI);

        this.encodingMI.setMnemonic('E');
        this.optionMenu.setMnemonic('O');
        this.ignoreEmptyTextNodeMI.setMnemonic('I');
        this.nsAwareMI.setMnemonic('N');

        return this.optionMenu;
    }

    /**
     *
     */
    private JMenu createHelpMenu() {
        this.helpMenu = new JMenu(I18N.get("menu.help"));
        this.wikiMI = new JMenuItem(I18N.get("menuitem.wiki"));
        this.aboutMI = new JMenuItem(I18N.get("menuitem.about"));

        this.helpMenu.add(this.wikiMI);
        this.helpMenu.addSeparator();
        this.helpMenu.add(this.aboutMI);

        this.wikiMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    try {
                        Desktop.getDesktop().
                            browse(new URL(I18N.get("wiki.url")).toURI());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        this.aboutMI.addActionListener(new ActionListener() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    showAboutDialog();
                }
            });

        return this.helpMenu;
    }

    /**
     *
     */
    private void loadPlugins() {
        File plugins = new File("plugins");
        if (plugins.exists()) {
            File[] files = plugins.listFiles(new FilenameFilter() {
                    /** */
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".jar");
                    }
                });
            if (files.length == 0) {
                return;
            }

            for (File f : files) {
                try {
                    addClasspath(f.toURI().toURL());
                } catch (Exception e) {
                    // MalformedURLException, NoSuchMethodException
                    // IllegalAccessException, InvocationTargetException
                    e.printStackTrace();
                }
                try {
                    findPluginClass(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param url
     */
    private void addClasspath(URL url) throws NoSuchMethodException,
                                              IllegalAccessException,
                                              InvocationTargetException {
        try {
            Method method =
                URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(getClass().getClassLoader(), new Object[]{url});
        } catch (NoSuchMethodException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw e;
        }
    }

    /**
     *
     */
    private void showAboutDialog() {
        String version = I18N.get("dialog.4",
                                  "Shigehiro Soejima",
                                  "mightyfrog.gc@gmail.com",
                                  "@TIMESTAMP@");
        JOptionPane.showMessageDialog(this, version);
    }

    /**
     *
     * @param message
     */
    private void showMessageDialog(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    /**
     *
     */
    private void showReloadRequiredDialog() {
        if (TREE.getRowCount() != 0) {
            showMessageDialog(I18N.get("dialog.7"));
        }
    }

    /**
     *
     */
    private int showYesNoDialog(String message) {
        String title = UIManager.getString("OptionPane.titleText",
                                           I18N.getLocale());
        return JOptionPane.showConfirmDialog(this, message, title,
                                             JOptionPane.YES_NO_OPTION);
    }

    //
    //
    //

    /**
     *
     */
    private static class IconButton extends JButton {
        /**
         *
         * @param icon
         * @param toolTipText
         */
        public IconButton(ImageIcon icon, String toolTipText) {
            super(icon);
            setToolTipText(toolTipText);
        }

        /** */
        @Override
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            dim.width = dim.height;
            return dim;
        }
    }
}
