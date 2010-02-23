package org.mightyfrog.util.xpathtester;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 */
class XPathTree extends JTree {
    //
    private XPathTree.RootNode rootNode = null;

    //
    private Document document = null;
    private NodeList nodeList = null;

    //
    private List<TreePath> matchedList = null;

    //
    private boolean ignoreEOL = true;
    private boolean namespaceAware = false;

    //
    private final XPath XPATH = XPathFactory.newInstance().newXPath();

    //
    private boolean treeExpanding = false;

    //
    private String encoding = "UTF-8";

    {
        getActionMap().put("cancelTreeExpansion", new AbstractAction() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    setTreeExpanding(false);
                }
            });
        getActionMap().put("findNext", new AbstractAction() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    findNext();
                }
            });
        getActionMap().put("findPrevious", new AbstractAction() {
                /** */
                @Override
                public void actionPerformed(ActionEvent evt) {
                    findPrevious();
                }
            });
        InputMap im = getInputMap();
        im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke("pressed ESCAPE"), "cancelTreeExpansion");
        im.put(KeyStroke.getKeyStroke("pressed F3"), "findNext");
        im.put(KeyStroke.getKeyStroke("shift pressed F3"), "findPrevious");
    }

    //
    private final Map<String, String> NS_MAP = new HashMap<String, String>();

    /**
     * Constructs a XPathTester.
     *
     */
    public XPathTree() {
        setCellRenderer(new CellRenderer());
        ((DefaultTreeModel) getModel()).setRoot(this.rootNode);

        setTransferHandler(new XMLStringTransferHandler());

        XPATH.setNamespaceContext(new NamespaceContext() {
                /** */
                @Override
                public String getNamespaceURI(String prefix) {
                    String uri = null;
                    if (prefix.length() == 0) {
                        uri = NS_MAP.get("xmlns");
                    } else {
                        uri = NS_MAP.get("xmlns:" + prefix);
                    }

                    return uri;
                }

                /** */
                @Override
               public String getPrefix(String namespaceURI) {
                    return null; // not used
                }

                /** */
                @Override
               public Iterator getPrefixes(String namespaceURI) {
                    return null; // not used
                }
            });
    }

    /** */
    @Override
    public void paintComponent(Graphics g) {
        if (!isTreeExpanding()) {
            super.paintComponent(g);
        }
    }

    //
    //
    //

    /**
     *
     */
    void findNext() {
        if (getMatchedList() != null && getMatchedList().size() != 0) {
            TreePath path = getSelectionPath();
            int index = getMatchedList().indexOf(path);
            if (index == getMatchedList().size() - 1) {
                path = getMatchedList().get(0);
            } else {
                path = getMatchedList().get(index + 1);
            }
            setSelectionPath(path);
            Rectangle rect = getPathBounds(path);
            rect.x = 0;
            scrollRectToVisible(rect);
        }
    }

    /**
     *
     */
    void findPrevious() {
        if (this.matchedList != null && getMatchedList().size() != 0) {
            TreePath path = getSelectionPath();
            int index = getMatchedList().indexOf(path);
            if (index == 0) {
                path = getMatchedList().get(getMatchedList().size() - 1);
            } else {
                path = getMatchedList().get(index - 1);
            }
            setSelectionPath(path);
            Rectangle rect = getPathBounds(path);
            rect.x = 0;
            scrollRectToVisible(rect);
        }
    }

    /**
     *
     * @param encoding
     */
    void setEncoding(String encoding) {
        try {
            Charset.forName(encoding);
            this.encoding = encoding;
        } catch (UnsupportedCharsetException e) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                          I18N.get("dialog.6", encoding),
                                          e.getClass().getName(),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *
     */
    String getEncoding() {
        return this.encoding;
    }

    /**
     *
     * @param ignoreEOL
     */
    void setIgnoreEOL(boolean ignoreEOL) {
        this.ignoreEOL = ignoreEOL;
    }

    /**
     *
     */
    boolean getIgnoreEOL() {
        return this.ignoreEOL;
    }

    /**
     *
     * @param namespaceAware
     */
    void setNamespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
    }

    /**
     *
     */
    boolean getNamespaceAware() {
        return this.namespaceAware;
    }

    /**
     *
     * @param document
     */
    void load(Document document) {
        this.document = document;
        this.rootNode = new XPathTree.RootNode(this.document);
        ((DefaultTreeModel) getModel()).setRoot(this.rootNode);


        if (getNamespaceAware()) {
            initNamespaceContext(this.document);
        }

        buildTree(this.rootNode, this.document);
        expandAllRows();
    }

    /**
     *
     * @param in
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    void load(InputStream in) throws IOException, SAXException {
        InputStreamReader reader = null;
        try {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(getNamespaceAware());
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setCoalescing(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            reader = new InputStreamReader(in, getEncoding());
            this.document = builder.parse(new InputSource(reader));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw e;
        } catch (SAXException e) {
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        this.rootNode = new XPathTree.RootNode(this.document);
        ((DefaultTreeModel) getModel()).setRoot(this.rootNode);

        if (getNamespaceAware()) {
            initNamespaceContext(this.document);
        }

        buildTree(this.rootNode, this.document);
        expandAllRows();
    }

    /**
     *
     * @param document
     */
    private void initNamespaceContext(Document document) {
        NS_MAP.clear();
        try {
            NodeList nodeList =
                (NodeList) XPATH.evaluate("//namespace::*", document,
                                          XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String prefix = node.getNodeName();
                if (!NS_MAP.containsKey(prefix)) {
                    NS_MAP.put(prefix, node.getNodeValue());
                }
            }
            if (NS_MAP.get(XMLConstants.XMLNS_ATTRIBUTE) == null) {
                NS_MAP.put(XMLConstants.XMLNS_ATTRIBUTE,
                           XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
            }
            if (NS_MAP.get(XMLConstants.DEFAULT_NS_PREFIX) == null) {
                NS_MAP.put(XMLConstants.DEFAULT_NS_PREFIX,
                           XMLConstants.NULL_NS_URI);
            }
            NS_MAP.put("xmlns:fn", "http://www.w3.org/2005/xpath-functions");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param path
     */
    @SuppressWarnings("unchecked")
    void expandAllRows(TreePath path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        if (!node.isLeaf() && node.getChildCount() >= 0) {
            Enumeration<XMLNode> enm = node.children();
            while (enm.hasMoreElements()) {
                expandAllRows(path.pathByAddingChild(enm.nextElement()));
            }
        }
        if (isTreeExpanding()) {
            expandPath(path);
        }
    }

    /**
     *
     */
    void expandAllRows() {
        SwingWorker<Void, Void> task = new SwingWorker<Void, Void>() {
            /** */
            @Override
            protected Void doInBackground() {
                setStatus(I18N.get("status.2"));
                toggleCursor(true);
                setTreeExpanding(true);
                expandAllRows(XPathTree.this.getPathForRow(0));

                return null;
            }

            /** */
            @Override
            protected void done() {
                setTreeExpanding(false);
                repaint();
                scrollRowToVisible(0);
                toggleCursor(false);
                setStatus(I18N.get("status.ok"));
            }
        };
        task.execute();
    }

    /**
     *
     */
    Document getDocument() {
        return this.document;
    }

    /**
     *
     * @param exp
     * @return the number of mached nodes or -1 if no document is loaded
     * @throws java.lang.Exception
     */
    int evaluate(String exp) throws Exception {
        if (getDocument() == null) {
            return -1;
        }
        try {
            setMessage("XPath: " + exp + "\n");
            long ss = System.currentTimeMillis();
            this.nodeList = (NodeList) XPATH.evaluate(exp, getDocument(),
                                                      XPathConstants.NODESET);
            System.out.println("execution time: " +
                               (System.currentTimeMillis() - ss) + "ms");
            for (int i = 0; i < this.nodeList.getLength(); i++) {
                Node node = this.nodeList.item(i);
                if (node instanceof Element) {
                    setMessage(I18N.get("msg.element", node.getNodeName()));
                } else if (node instanceof Attr) {
                    String prefix = node.getNodeName();
                    if (prefix.toLowerCase().startsWith("xml")) {
                        setMessage(I18N.get("msg.namespace", prefix, node.getNodeValue()));
                    } else {
                        setMessage(I18N.get("msg.attribute", prefix, node.getNodeValue()));
                    }
                } else if (node instanceof Text) {
                    setMessage(I18N.get("msg.text", node.getNodeValue()));
                } else if (node instanceof Comment) {
                    setMessage(I18N.get("msg.comment", node.getNodeValue()));
                } else if (node instanceof ProcessingInstruction) {
                    setMessage(I18N.get("msg.processinst", node.getNodeValue()));
                } else if (node instanceof Document) {
                    setMessage(I18N.get("msg.document", node.getNodeName(), node.getNodeValue()));
                } else { // namespace
                    setMessage(node.toString());
                }
            }
        } catch (Exception e) {
            // com.sun.org.apache.xpath.internal.XPathException
            // XPathExpressionException
            // TransformerException
            throw e;
        } finally {
            setMessage("\n");
        }

        repaint();

        int length = getNodeList().getLength();

        if (length != 0) {
            this.matchedList = new ArrayList<TreePath>();
            storeMatchedNodes();
        } else {
            this.matchedList = null;
        }

        return length;
    }

    /**
     *
     * @param exp
     */
    String evaluateFunction(String exp) throws TransformerException,
                                               XPathExpressionException {
        String s = null;
        try {
            s = XPATH.evaluate(exp, getDocument());
        } catch (XPathExpressionException e) {
            throw e;
        }

        return s;
    }

    //
    //
    //

    /**
     * Builds the tree.
     *
     * @param tn tree node
     * @param xn xml node
     */
    protected void buildTree(XMLNode tn, Node xn) {
        NodeList list = xn.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node cxn = list.item(i);
            XMLNode ctn = new XPathTree.StartNode(cxn);
            if (cxn instanceof Text) {
                if (getIgnoreEOL()) {
                    String text = cxn.getTextContent();
                    if (text != null && text.trim().length() != 0) {
                        tn.add(new TextNode(cxn));
                    }
                } else {
                    tn.add(new TextNode(cxn));
                }
            } else if (cxn instanceof Comment) {
                tn.add(new XPathTree.CommentNode(cxn));
            } else if (cxn instanceof ProcessingInstruction) {
                tn.add(new XPathTree.PINode(cxn));
            } else {
                tn.add(ctn);
                if (cxn.hasChildNodes()) {
                    tn.add(new XPathTree.EndNode(cxn));
                }
            }

            buildTree(ctn, cxn);
        }
    }

    //
    //
    //

    //
    private SwingWorker<Void, TreePath> task = null;

    /**
     *
     */
    boolean cancelStoreTask() {
        boolean b = true;
        if (this.task != null) {
            if (this.task.isDone()) {
                b = true;
            } else {
                b = this.task.cancel(true);
            }
        }
        if (b) {
            this.nodeList = null;
            this.matchedList = null;
            repaint();
        }

        return b;
    }

    /**
     *
     */
    private void storeMatchedNodes() {
        this.task = new SwingWorker<Void, TreePath>() {
            //
            private boolean found = false;

            /** */
            @Override
            @SuppressWarnings("unchecked")
            protected Void doInBackground() {
                long ss = System.currentTimeMillis();
                Enumeration<XMLNode> enm =
                    XPathTree.this.rootNode.depthFirstEnumeration();
                int length = getNodeList().getLength();
                int start = 0;
                while (enm.hasMoreElements()) { // slow slow
                    XMLNode tn = enm.nextElement();
                    if (tn instanceof EndNode) {
                        continue;
                    }
                    Node xn = tn.getUserObject();
                    for (int i = start; i < length; i++) {
                        Node nn = getNodeList().item(i);
                        if (nn instanceof Attr) {
                            nn = ((Attr) nn).getOwnerElement();
                        }
                        if (xn.isSameNode(nn)) {
                            start++;
                            publish(new TreePath(tn.getPath()));
                            break;
                        }
                    }
                }
                System.out.println(System.currentTimeMillis() - ss);
                return null;
            }

            /** */
            @Override
            protected void process(List<TreePath> chunks) {
                if (getMatchedList() != null) {
                    getMatchedList().addAll(chunks);
                    if (!this.found) {
                        scrollToTheFirstMatch();
                        this.found = true;
                    }
                }
            }

        };
        task.execute();
    }

    /**
     *
     */
    private void scrollToTheFirstMatch() {
        TreePath path = getMatchedList().get(0);
        Rectangle rect = getPathBounds(path);
        rect.x = 0;
        scrollRectToVisible(rect);
        setSelectionPath(path);
    }

    /**
     *
     */
    private boolean isTreeExpanding() {
        return this.treeExpanding;
    }

    /**
     *
     * @param treeExpanding
     */
    private void setTreeExpanding(boolean treeExpanding) {
        this.treeExpanding = treeExpanding;
    }

    /**
     *
     */
    private NodeList getNodeList() {
        return this.nodeList;
    }

    /**
     *
     */
    private List<TreePath> getMatchedList() {
        return this.matchedList;
    }

    //
    //
    //

    /**
     *
     */
    private class CellRenderer extends DefaultTreeCellRenderer {
        /** */
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                                               leaf, row, hasFocus);
            setIcon(null);

            NodeList list = getNodeList();
            if (list == null) {
                setForeground(UIManager.getColor("Tree.foreground"));
                return this;
            }

            XMLNode tn = (XMLNode) value;
            Node node = tn.getUserObject();
            for (int i = 0; i < list.getLength(); i++) {
                Node xnode = list.item(i);
                if (xnode instanceof Attr) {
                    xnode = ((Attr) xnode).getOwnerElement();
                }
                //if (node.isEqualNode(xnode)) {
                if (node.isSameNode(xnode)) {
                    if (sel) {
                        setForeground(SystemColor.text);
                    } else {
                        setForeground(Color.BLUE);
                    }
                    break;
                }
            }

            return this;
        }
    }

    /**
     *
     */
    private static class RootNode extends XMLNode {
        /**
         *
         * @param node
         */
        public RootNode(Node node) {
            super(node);

            Document doc = (Document) node;
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"" + doc.getXmlVersion());
            if (doc.getXmlEncoding() != null) {
                sb.append("\" encoding=\"" + doc.getXmlEncoding() + "");
            }
            if (doc.getXmlStandalone()) {
                sb.append("\" standalone=\"yes");
            }
            sb.append("\" ?>");
            setValue(sb.toString());
        }
    }

    /**
     *
     */
    private static class StartNode extends XMLNode {
        /**
         *
         * @param node
         */
        public StartNode(Node node) {
            super(node);

            StringBuilder sb = new StringBuilder();
            if (node.hasAttributes()) {
                NamedNodeMap attrMap = node.getAttributes();
                for (int i = 0; i < attrMap.getLength(); i++) {
                    Node an = attrMap.item(i);
                    sb.append(" ");
                    sb.append(an.getNodeName());
                    sb.append("=\"");
                    sb.append(an.getNodeValue());
                    sb.append("\"");
                }
            }

            String str = "<" + node.getNodeName() + sb.toString();
            if (node.hasChildNodes()) {
                str += ">";
            } else {
                str += "/>";
            }

            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4399545
            if (str.equals("<html>")) {
                str = " <html>";
            }

            setValue(str);
        }
    }

    /**
     *
     */
    private static class EndNode extends XMLNode {
        /**
         *
         * @param node
         */
        public EndNode(Node node) {
            super(node);

            setValue("</" + node.getNodeName() + ">");
        }
    }

    /**
     * Processing Instruction Node
     *
     */
    private static class PINode extends XMLNode {
        /**
         *
         * @param node
         */
        public PINode(Node node) {
            super(node);

            String data = ((ProcessingInstruction) node).getData();
            setValue("<?" + node.getNodeName() + " " + data + "?>");
        }
    }

    /**
     * Processing Instruction Node
     *
     */
    private static class CommentNode extends XMLNode {
        /**
         *
         * @param node
         */
        public CommentNode(Node node) {
            super(node);

            setValue("<!--" + node.getNodeValue() + "-->");
        }
    }

    /**
     *
     */
    private static class TextNode extends XMLNode {
        /**
         *
         * @param node
         */
        public TextNode(Node node) {
            super(node);

            setValue(((Text) node).getWholeText());
        }
    }

    /**
     *
     */
    private static class XMLNode extends DefaultMutableTreeNode {
        //
        private String value = null;
        private Node node = null;

        /**
         *
         * @param node
         */
        public XMLNode(Node node) {
            this.node = node;
        }

        /** */
        @Override
        public Node getUserObject() { // covariant
            return this.node;
        }

        /** */
        @Override
        public String toString() {
            return this.value;
        }

        /**
         *
         * @param value don't confuse this with the user object.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     *
     * @param text
     */
    private void setMessage(String text) {
        ((XPathTester) SwingUtilities.getRoot(this)).setMessage(text);
    }

    /**
     *
     * @param text
     */
    private void setStatus(String text) {
        ((XPathTester) SwingUtilities.getRoot(this)).setStatus(text);
    }

    /**
     *
     * @param running
     */
    private void toggleCursor(boolean running) {
        ((XPathTester) SwingUtilities.getRoot(this)).toggleCursor(running);
    }

    /**
     *
     */
    private class XMLStringTransferHandler extends TransferHandler {
        /** */
        @Override
        public boolean canImport(JComponent comp,
                                 DataFlavor[] transferFlavors) {
            return !XPathTree.this.treeExpanding;
        }

        /** */
        @Override
        public boolean importData(JComponent comp, Transferable t) {
            try {
                toggleCursor(true);
                Class c = t.getTransferDataFlavors()[0].getRepresentationClass();
                DataFlavor flavor = t.getTransferDataFlavors()[0];
                Object obj = t.getTransferData(flavor);
                InputStream in = null;
                if (flavor.isFlavorJavaFileListType()) {
                    in = new FileInputStream((File) ((List) obj).get(0));
                } else if (c == URL.class) {
                    in = new ProgressMonitorInputStream(XPathTree.this,
                                                        "Loading...",
                                                        ((URL) obj).openStream());
                } else {
                    String s = (String) t.getTransferData(DataFlavor.stringFlavor);
                    List<File> list = textURIListToFileList(s);
                    if (list.size() == 0) {
                        in = new ByteArrayInputStream(s.getBytes("utf-8"));
                    } else {
                        in = new FileInputStream(list.get(0));
                    }
                }
                load(new BufferedInputStream(in));
            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            } catch (SAXException e) {
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                              e.getLocalizedMessage(),
                                              e.getClass().getName(),
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            } finally {
                toggleCursor(false);
            }

            return true;
        }

        /**
         *
         */
        private final DataFlavor createURIListFlavor() {
            DataFlavor df = null;
            try {
                df = new DataFlavor("text/uri-list;class=java.lang.String");
            } catch (ClassNotFoundException e) {
                // shouldn't happen
            }

            return df;
        }

        /**
         *
         * @param uriList
         */
        private final List<File> textURIListToFileList(String uriList) {
            List<File> list = new ArrayList<File>(1);
            StringTokenizer st = new StringTokenizer(uriList, "\r\n");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.startsWith("#")) {
                    // the line is a comment (as per the RFC 2483)
                    continue;
                }
                try {
                    URI uri = new URI(s);
                    File file = new File(uri);
                    if (file.length() != 0) {
                        list.add(file);
                    }
                } catch (URISyntaxException e) {
                    // ignore
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }

            return list;
        }
    }
}
