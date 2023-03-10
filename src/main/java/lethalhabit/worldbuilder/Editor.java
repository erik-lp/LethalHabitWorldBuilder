package lethalhabit.worldbuilder;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.*;

import static lethalhabit.worldbuilder.Util.*;

public class Editor extends JFrame {
    
    private static final List<BufferedImage> OG_TILEMAP = new ArrayList<>();
    private static final List<BufferedImage> OG_LIQUID_TILEMAP = new ArrayList<>();
    private static final List<BufferedImage> OG_INTERACTABLE_TILEMAP = new ArrayList<>();
    
    private static List<BufferedImage> TILEMAP = new ArrayList<>();
    private static List<BufferedImage> LIQUID_TILEMAP = new ArrayList<>();
    private static List<BufferedImage> INTERACTABLE_TILEMAP = new ArrayList<>();
    private static final List<Integer> activeKeys = new ArrayList<>();
    
    private final EditorPane editorPane;
    private final Toolbar toolbar;
    private final Toolbar sidebarR;
    private final Toolbar sidebarL;
    
    private Map<Integer, Map<Integer, Tile>> importedWorldData = null;
    private int importedWorldOffsetX = 0;
    private int importedWorldOffsetY = 0;
    
    private boolean inferOrientation = true;
    
    public Editor() {
        loadTilemaps();
        setTitle("Lethal Habit - World Builder");
        setSize(1300, 800);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        setResizable(true);
        setLayout(new BorderLayout());
        editorPane = new EditorPane();
        toolbar = new Toolbar(TILEMAP, true, true);
        sidebarR = new Toolbar(LIQUID_TILEMAP, false, true);
        sidebarL = new Toolbar(INTERACTABLE_TILEMAP, false, false);
        add(editorPane, BorderLayout.CENTER);
        add(toolbar, BorderLayout.PAGE_START);
        add(sidebarR, BorderLayout.LINE_END);
        add(sidebarL, BorderLayout.LINE_START);
        setVisible(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_K -> {
                        // toggle toolbar (tiles)
                        toolbar.toggle();
                    }
                    case KeyEvent.VK_L -> {
                        // toggle sidebar (interactables)
                        sidebarL.toggle();
                    }
                    case KeyEvent.VK_O -> {
                        // toggle sidebar (liquids)
                        sidebarR.toggle();
                    }
                    case KeyEvent.VK_P -> {
                        // toggle orientation inferring mode
                        inferOrientation = !inferOrientation;
                    }
                    case KeyEvent.VK_RIGHT -> {
                        // move imported world right
                        if (importedWorldData != null) {
                            importedWorldOffsetX += 1;
                        }
                    }
                    case KeyEvent.VK_LEFT -> {
                        // move imported world left
                        if (importedWorldData != null) {
                            importedWorldOffsetX -= 1;
                        }
                    }
                    case KeyEvent.VK_UP -> {
                        // move imported world up
                        if (importedWorldData != null) {
                            importedWorldOffsetY -= 1;
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        // move imported world down
                        if (importedWorldData != null) {
                            importedWorldOffsetY += 1;
                        }
                    }
                    case KeyEvent.VK_Q -> {
                        // move toolbar selection left
                        toolbar.prepareSelection();
                        if (inferOrientation) {
                            int tileGroupCount = TILEMAP.size() / WorldBuilder.TILE_GROUP_SIZE;
                            int currentGroup = Math.max(0, toolbar.getSelection()) / WorldBuilder.TILE_GROUP_SIZE;
                            toolbar.select((((currentGroup - 1) % tileGroupCount + tileGroupCount) % tileGroupCount) * WorldBuilder.TILE_GROUP_SIZE);
                        } else {
                            toolbar.select((Math.max(-1, (toolbar.getSelection() - 1)) % TILEMAP.size() + TILEMAP.size()) % TILEMAP.size());
                        }
                    }
                    case KeyEvent.VK_E -> {
                        // move toolbar selection right
                        toolbar.prepareSelection();
                        if (inferOrientation) {
                            int tileGroupCount = TILEMAP.size() / WorldBuilder.TILE_GROUP_SIZE;
                            int currentGroup = toolbar.getSelection() / WorldBuilder.TILE_GROUP_SIZE;
                            toolbar.select(((currentGroup + 1) % tileGroupCount) * WorldBuilder.TILE_GROUP_SIZE);
                        } else {
                            toolbar.select((toolbar.getSelection() + 1) % TILEMAP.size());
                        }
                    }
                    case KeyEvent.VK_C -> {
                        // toggle toolbar selection
                        toolbar.toggleSelection();
                    }
                    case KeyEvent.VK_V -> {
                        // toggle sidebar selection
                        sidebarR.toggleSelection();
                    }
                    case KeyEvent.VK_B -> {
                        sidebarL.toggleSelection();
                    }
                    case KeyEvent.VK_SPACE -> {
                        // move sidebar selection down
                        sidebarR.prepareSelection();
                        sidebarR.select((sidebarR.getSelection() + 1) % LIQUID_TILEMAP.size());
                    }
                    case KeyEvent.VK_G -> {
                        // toggle grid drawing
                        editorPane.drawGrid = !editorPane.drawGrid;
                    }
                    case KeyEvent.VK_PERIOD -> {
                        // zoom in
                        int previousTileSize = WorldBuilder.TILE_SIZE;
                        WorldBuilder.TILE_SIZE += 5;
                        TILEMAP = OG_TILEMAP.stream().map(img -> Scalr.resize(img, WorldBuilder.TILE_SIZE, WorldBuilder.TILE_SIZE)).toList();
                        LIQUID_TILEMAP = OG_LIQUID_TILEMAP.stream().map(img -> Scalr.resize(img, WorldBuilder.TILE_SIZE, WorldBuilder.TILE_SIZE)).toList();
                        INTERACTABLE_TILEMAP = OG_INTERACTABLE_TILEMAP.stream().map(img -> Scalr.resize(img, WorldBuilder.TILE_SIZE, WorldBuilder.TILE_SIZE)).toList();
                        editorPane.camera.setPosition((editorPane.camera.getPosition().x() / previousTileSize) * WorldBuilder.TILE_SIZE, (editorPane.camera.getPosition().y() / previousTileSize) * WorldBuilder.TILE_SIZE);
                    }
                    case KeyEvent.VK_COMMA -> {
                        // zoom out
                        WorldBuilder.TILE_SIZE = Math.max(5, WorldBuilder.TILE_SIZE - 5);
                        TILEMAP = OG_TILEMAP.stream().map(img -> Scalr.resize(img, WorldBuilder.TILE_SIZE, WorldBuilder.TILE_SIZE)).toList();
                        LIQUID_TILEMAP = OG_LIQUID_TILEMAP.stream().map(img -> Scalr.resize(img, WorldBuilder.TILE_SIZE, WorldBuilder.TILE_SIZE)).toList();
                        INTERACTABLE_TILEMAP = OG_INTERACTABLE_TILEMAP.stream().map(img -> Scalr.resize(img, WorldBuilder.TILE_SIZE, WorldBuilder.TILE_SIZE)).toList();
                    }
                    case KeyEvent.VK_T -> {
                        // teleport
                        JTextField xField = new JTextField(5);
                        JTextField yField = new JTextField(5);
                        ((PlainDocument) xField.getDocument()).setDocumentFilter(integerInputFilter());
                        ((PlainDocument) yField.getDocument()).setDocumentFilter(integerInputFilter());
                        JPanel inputPanel = new JPanel();
                        inputPanel.add(new JLabel("X "));
                        inputPanel.add(xField);
                        inputPanel.add(Box.createHorizontalStrut(15));
                        inputPanel.add(new JLabel("Y "));
                        inputPanel.add(yField);
                        int result = JOptionPane.showConfirmDialog(Editor.this, inputPanel, "Teleport to", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            int newX = Math.max(editorPane.minimumX(), Integer.parseInt(xField.getText()));
                            int newY = Math.max(editorPane.minimumY(), Integer.parseInt(yField.getText()));
                            editorPane.camera.setPosition(newX, newY);
                        }
                    }
                    case KeyEvent.VK_M -> {
                        // show map
                        BufferedImage minimap = generateMinimap();
                        createMinimapWindow(minimap);
                    }
                    case KeyEvent.VK_X -> {
                        // undo
                        if (!editorPane.recentWorldStates.isEmpty()) {
                            editorPane.recentlyUndoneWorldStates.push(copyWorldData(WorldBuilder.INSTANCE.getWorldData()));
                            WorldBuilder.INSTANCE.setWorldData(copyWorldData(editorPane.recentWorldStates.pop()));
                        }
                    }
                    case KeyEvent.VK_Y -> {
                        // redo
                        if (!editorPane.recentlyUndoneWorldStates.isEmpty()) {
                            editorPane.recentWorldStates.push(copyWorldData(WorldBuilder.INSTANCE.getWorldData()));
                            WorldBuilder.INSTANCE.setWorldData(editorPane.recentlyUndoneWorldStates.pop());
                        }
                    }
                    case KeyEvent.VK_F3 -> {
                        // toggle show position in corner
                        editorPane.showPosition = !editorPane.showPosition;
                    }
                    case KeyEvent.VK_F4 -> {
                        // show tile indices
                        editorPane.showTileIndices = !editorPane.showTileIndices;
                    }
                    case KeyEvent.VK_I -> {
                        // open import world dialog
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Import world file");
                        fileChooser.setFileFilter(jsonFileFilter());
                        int result = fileChooser.showOpenDialog(Editor.this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            importedWorldData = readWorldData(fileChooser.getSelectedFile());
                        }
                    }
                    case KeyEvent.VK_ENTER -> {
                        // merge imported world data into world data
                        editorPane.recentWorldStates.push(copyWorldData(WorldBuilder.INSTANCE.getWorldData()));
                        Map<Integer, Map<Integer, Tile>> newWorldData = WorldBuilder.INSTANCE.getWorldData();
                        for (Map.Entry<Integer, Map<Integer, Tile>> entry : importedWorldData.entrySet()) {
                            int columnIndex = entry.getKey() + importedWorldOffsetX;
                            Map<Integer, Tile> currentColumn = WorldBuilder.INSTANCE.getWorldData().getOrDefault(columnIndex, new HashMap<>());
                            for (Map.Entry<Integer, Tile> entry1 : entry.getValue().entrySet()) {
                                int rowIndex = entry1.getKey() + importedWorldOffsetY;
                                Tile tile = entry1.getValue();
                                if (columnIndex >= 0 && rowIndex >= 0) {
                                    currentColumn.put(rowIndex, tile);
                                }
                            }
                            newWorldData.put(columnIndex, currentColumn);
                        }
                        WorldBuilder.INSTANCE.setWorldData(newWorldData);
                        importedWorldData = null;
                        importedWorldOffsetX = 0;
                        importedWorldOffsetY = 0;
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        // remove imported world
                        importedWorldData = null;
                        importedWorldOffsetX = 0;
                        importedWorldOffsetY = 0;
                    }
                    case KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 -> {
                        // select n-th tile group
                        toolbar.prepareSelection();
                        toolbar.select((e.getKeyCode() - 0x31) * WorldBuilder.TILE_GROUP_SIZE);
                    }
                    case KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9 -> {
                        // select n-th liquid group
                        sidebarR.prepareSelection();
                        sidebarR.select((e.getKeyCode() - 0x61) * WorldBuilder.LIQUID_GROUP_SIZE);
                    }
                    case KeyEvent.VK_J -> {
                        // save
                        saveDialog(WorldBuilder.INSTANCE::saveWorldData, null, false, jsonFileFilter(), "Save world file", Editor.this);
                    }
                    default -> {
                        // add keys that respond to holding to the list
                        activeKeys.add(e.getKeyCode());
                    }
                }
            }
            
            public void keyReleased(KeyEvent e) {
                activeKeys.removeIf(key -> key == e.getKeyCode());
            }
        });
    }
    
    private static BufferedImage generateMinimap() {
        Integer maxX = WorldBuilder.INSTANCE.getWorldData().keySet().stream().max(Integer::compareTo).orElse(null);
        Integer maxY = WorldBuilder.INSTANCE.getWorldData().values().stream().flatMap(column -> column.keySet().stream()).max(Integer::compareTo).orElse(null);
        
        if (maxX == null || maxY == null) {
            return null;
        } else {
            int tilePixelSize = Math.min((int) (WorldBuilder.WIDTH * 0.9) / maxX, (int) (WorldBuilder.HEIGHT * 0.8) / maxY);
            BufferedImage map = new BufferedImage((maxX + 1) * tilePixelSize, (maxY + 1) * tilePixelSize, BufferedImage.TYPE_INT_ARGB);
            for (Map.Entry<Integer, Map<Integer, Tile>> column : WorldBuilder.INSTANCE.getWorldData().entrySet()) {
                for (Map.Entry<Integer, Tile> row : column.getValue().entrySet()) {
                    Tile tile = row.getValue();
                    Point position = new Point(column.getKey() * tilePixelSize, row.getKey() * tilePixelSize);
                    if (tile.liquid >= 0) {
                        map.getGraphics().drawImage(OG_LIQUID_TILEMAP.get(tile.liquid), position.x(), position.y(), tilePixelSize, tilePixelSize, null);
                    }
                    if (tile.block >= 0) {
                        map.getGraphics().drawImage(OG_TILEMAP.get(tile.block), position.x(), position.y(), tilePixelSize, tilePixelSize, null);
                    }
                }
            }
            map.getGraphics().dispose();
            return map;
        }
    }
    
    private static void loadTilemaps() {
        load("/tiles/tile", OG_TILEMAP);
        TILEMAP = new ArrayList<>(OG_TILEMAP);
        load("/liquids/liquid", OG_LIQUID_TILEMAP);
        LIQUID_TILEMAP = new ArrayList<>(OG_LIQUID_TILEMAP);
        load("/interactables/interactable", OG_INTERACTABLE_TILEMAP);
        INTERACTABLE_TILEMAP = new ArrayList<>(OG_INTERACTABLE_TILEMAP);
    }
    
    private static void load(String fromPath, List<BufferedImage> to) {
        for (int i = 0; ; i++) {
            try {
                InputStream stream = Editor.class.getResourceAsStream(fromPath + i + ".png");
                to.add(ImageIO.read(stream));
            } catch (Exception ex) {
                return;
            }
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (WorldBuilder.INSTANCE.getWorldData().equals(WorldBuilder.INSTANCE.getLastSavedWorldData())) {
            System.exit(0);
        } else {
            saveDialog((file) -> {
                int success = WorldBuilder.INSTANCE.saveWorldData(file);
                System.exit(success);
            }, Editor::new, true, jsonFileFilter(), "Save world file", Editor.this);
        }
    }
    
    public class EditorPane extends JPanel {
        
        private final Camera camera = new Camera(minimumX(), minimumY(), 2);
        
        private final Stack<Map<Integer, Map<Integer, Tile>>> recentWorldStates = new Stack<>();
        private final Stack<Map<Integer, Map<Integer, Tile>>> recentlyUndoneWorldStates = new Stack<>();
        
        private Point mousePosition;
        private boolean mouseInPane = true;
        private int activeMouseButton = -1;
        private int chunkX = -1;
        private int chunkY = -1;
        
        private boolean drawGrid = true;
        private boolean showPosition = false;
        private boolean showTileIndices = false;
        
        private long ticks = 0;
        
        public EditorPane() {
            addMouseWheelListener(new MouseInputAdapter() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    camera.setSpeed(camera.getSpeed() - e.getWheelRotation());
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    mousePosition = new Point(e.getPoint()).plus(0, WorldBuilder.TILE_SIZE);
                }
                
                public void mouseDragged(MouseEvent e) {
                    mouseMoved(e);
                }
            });
            addMouseListener(new MouseInputAdapter() {
                public void mousePressed(MouseEvent e) {
                    addUndoCheckpoint();
                    activeMouseButton = e.getButton();
                }
                
                public void mouseReleased(MouseEvent e) {
                    activeMouseButton = -1;
                }
                
                public void mouseEntered(MouseEvent e) {
                    mouseInPane = true;
                }
                
                public void mouseExited(MouseEvent e) {
                    mouseInPane = false;
                }
            });
        }
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setFont(g.getFont().deriveFont(16f).deriveFont(Font.BOLD));
            g.setColor(Color.BLACK);
            if (drawGrid) {
                // DRAW VERTICAL GRID LINES
                for (int i = 0; i <= WorldBuilder.WIDTH / WorldBuilder.TILE_SIZE; i++) {
                    int offset = camera.getPosition().x() % WorldBuilder.TILE_SIZE;
                    int x = i * WorldBuilder.TILE_SIZE - offset;
                    g.drawLine(x, 0, x, WorldBuilder.HEIGHT);
                }
                // DRAW HORIZONTAL GRID LINES
                for (int i = 0; i <= WorldBuilder.HEIGHT / WorldBuilder.TILE_SIZE; i++) {
                    int offset = camera.getPosition().y() % WorldBuilder.TILE_SIZE;
                    int y = i * WorldBuilder.TILE_SIZE - offset;
                    g.drawLine(0, y, WorldBuilder.WIDTH, y);
                }
            }
            // DRAW TILES
            int offsetX = camera.getPosition().x() / WorldBuilder.TILE_SIZE;
            int offsetY = camera.getPosition().y() / WorldBuilder.TILE_SIZE;
            for (int i = 0; i <= WorldBuilder.WIDTH / WorldBuilder.TILE_SIZE + offsetX; i++) {
                for (int j = 0; j <= WorldBuilder.HEIGHT / WorldBuilder.TILE_SIZE + offsetY; j++) {
                    int x = (i + (WorldBuilder.WIDTH / 2) / WorldBuilder.TILE_SIZE + 1) * WorldBuilder.TILE_SIZE - camera.getPosition().x();
                    int y = (j + (WorldBuilder.HEIGHT / 2) / WorldBuilder.TILE_SIZE) * WorldBuilder.TILE_SIZE - camera.getPosition().y();
                    boolean hovered = mouseInPane && mousePosition != null && mousePosition.x() >= x && mousePosition.x() <= x + WorldBuilder.TILE_SIZE && mousePosition.y() >= y + WorldBuilder.TILE_SIZE && mousePosition.y() <= y + 2 * WorldBuilder.TILE_SIZE;
                    BufferedImage blockImage = null, liquidImage = null, interactableImage = null;
                    Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().get(i);
                    if (column != null) {
                        Tile tile = column.get(j);
                        if (tile != null) {
                            if (tile.block >= 0) {
                                blockImage = TILEMAP.get(tile.block);
                            }
                            if (tile.liquid >= 0) {
                                liquidImage = LIQUID_TILEMAP.get(tile.liquid);
                            }
                            if (tile.interactable >= 0) {
                                interactableImage = INTERACTABLE_TILEMAP.get(tile.interactable);
                            }
                        }
                    }
                    if (liquidImage != null) {
                        g.drawImage(liquidImage, x, y, null);
                    }
                    if (blockImage != null) {
                        g.drawImage(blockImage, x, y, null);
                    }
                    if (interactableImage != null) {
                        g.drawImage(interactableImage, x, y, null);
                    }
                    if (hovered) {
                        if (toolbar.getSelection() >= 0) {
                            g.drawImage(transparentImage(TILEMAP.get(toolbar.getSelection()), 0.35f), x, y, null);
                        }
                        if (sidebarR.getSelection() >= 0) {
                            g.drawImage(transparentImage(LIQUID_TILEMAP.get(sidebarR.getSelection()), 0.35f), x, y, null);
                        }
                        if (sidebarL.getSelection() >= 0) {
                            g.drawImage(transparentImage(INTERACTABLE_TILEMAP.get(sidebarL.getSelection()), 0.35f), x, y, null);
                        }
                        chunkX = i;
                        chunkY = j;
                    }
                    if (importedWorldData != null) {
                        // draw imported map
                        BufferedImage importedTileImage = null, importedLiquidImage = null, importedInteractableImage = null;
                        Map<Integer, Tile> importedColumn = importedWorldData.get(i - importedWorldOffsetX);
                        if (importedColumn != null) {
                            Tile importedTile = importedColumn.get(j - importedWorldOffsetY);
                            if (importedTile != null) {
                                if (importedTile.block >= 0) {
                                    importedTileImage = TILEMAP.get(importedTile.block);
                                }
                                if (importedTile.liquid >= 0) {
                                    importedLiquidImage = LIQUID_TILEMAP.get(importedTile.liquid);
                                }
                                if (importedTile.interactable >= 0) {
                                    importedInteractableImage = INTERACTABLE_TILEMAP.get(importedTile.interactable);
                                }
                            }
                        }
                        if (importedLiquidImage != null) {
                            g.drawImage(transparentImage(importedLiquidImage, 0.5f), x, y, null);
                        }
                        if (importedTileImage != null) {
                            g.drawImage(transparentImage(importedTileImage, 0.5f), x, y, null);
                        }
                        if (importedInteractableImage != null) {
                            g.drawImage(transparentImage(importedInteractableImage, 0.5f), x, y, null);
                        }
                    }
                    if (showTileIndices) {
                        g.setFont(g.getFont().deriveFont(Font.BOLD, 16f * (float) WorldBuilder.TILE_SIZE / (float) WorldBuilder.OG_TILE_SIZE));
                        String string = i + " | " + j;
                        int stringX = x + (WorldBuilder.TILE_SIZE - g.getFontMetrics().stringWidth(string)) / 2;
                        int stringY = y + WorldBuilder.TILE_SIZE - (WorldBuilder.TILE_SIZE - g.getFontMetrics().getHeight()) / 2;
                        g.setColor(Color.BLUE);
                        g.drawString(i + " | " + j, stringX, stringY);
                    }
                }
            }
            if (showPosition) {
                g.setColor(Color.RED);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));
                g.drawString("X: " + camera.getPosition().x(), 5, 20);
                g.drawString("Y: " + camera.getPosition().y(), 5, 40);
            }
            repaint();
        }
        
        @Override
        public void repaint() {
            super.repaint();
            updateCamera();
            checkClicks();
            ticks++;
        }
        
        private int minimumX() {
            return (WorldBuilder.WIDTH + WorldBuilder.TILE_SIZE) / 2 - 8;
        }
        
        private int minimumY() {
            return (WorldBuilder.HEIGHT + WorldBuilder.TILE_SIZE) / 2 - 87;
        }
        
        private boolean canMoveLeft(int speed) {
            return camera.getPosition().x() - speed >= minimumX() - 2;
        }
        
        private boolean canMoveUp(int speed) {
            return camera.getPosition().y() - speed >= minimumY() - 2;
        }
        
        private void checkClicks() {
            if (chunkX >= 0 && chunkY >= 0) {
                switch (activeMouseButton) {
                    case 1 -> { // left click
                        Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().getOrDefault(chunkX, new HashMap<>());
                        Tile current = column.get(chunkY);
                        column.put(chunkY, new Tile(
                                toolbar.getSelection() >= 0 ? toolbar.getSelection() : (current == null ? -1 : current.block),
                                sidebarR.getSelection() >= 0 ? sidebarR.getSelection() : (current == null ? -1 : current.liquid),
                                sidebarL.getSelection() >= 0 ? sidebarL.getSelection() : (current == null ? -1 : current.interactable)
                        ));
                        WorldBuilder.INSTANCE.getWorldData().put(chunkX, column);
                        if (inferOrientation) {
                            WorldBuilder.INSTANCE.autoShapeChunk(chunkX, chunkY, false);
                        }
                    }
                    case 2 -> { // middle click
                        Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().get(chunkX);
                        if (column != null) {
                            Tile tile = column.get(chunkY);
                            if (tile != null) {
                                if (tile.block >= 0) {
                                    toolbar.prepareSelection();
                                    toolbar.select(tile.block);
                                }
                                if (tile.liquid >= 0) {
                                    sidebarR.prepareSelection();
                                    sidebarR.select(tile.liquid);
                                }
                            }
                        }
                    }
                    case 3 -> { // right click
                        Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().get(chunkX);
                        if (column != null) {
                            column.remove(chunkY);
                            if (column.isEmpty()) {
                                WorldBuilder.INSTANCE.getWorldData().remove(chunkX);
                            } else {
                                WorldBuilder.INSTANCE.getWorldData().put(chunkX, column);
                            }
                        }
                        if (inferOrientation) {
                            WorldBuilder.INSTANCE.autoShapeChunk(chunkX, chunkY, false);
                        }
                    }
                }
            }
        }
        
        private void updateCamera() {
            if (activeKeys != null && camera != null) {
                int speed = camera.getSpeed();
                if (activeKeys.contains(KeyEvent.VK_SHIFT)) {
                    speed = Camera.MIN_SPEED;
                } else if (activeKeys.contains(KeyEvent.VK_ALT)) {
                    speed = Camera.MAX_SPEED;
                }
                if (activeKeys.contains(KeyEvent.VK_A) && canMoveLeft(speed)) {
                    camera.moveX(-speed);
                } else if (activeKeys.contains(KeyEvent.VK_D)) {
                    camera.moveX(speed);
                }
                if (activeKeys.contains(KeyEvent.VK_W) && canMoveUp(speed)) {
                    camera.moveY(-speed);
                } else if (activeKeys.contains(KeyEvent.VK_S)) {
                    camera.moveY(speed);
                }
            }
        }
        
        private void addUndoCheckpoint() {
            Map<Integer, Map<Integer, Tile>> currentWorld = copyWorldData(WorldBuilder.INSTANCE.getWorldData());
            if (recentWorldStates.isEmpty() || !currentWorld.equals(recentWorldStates.peek())) {
                recentWorldStates.push(currentWorld);
            }
        }
        
    }
    
    public class Toolbar extends JScrollPane {
        
        public static final int MARGIN = 15;
        
        private final List<BufferedImage> resources;
        private final boolean horizontal;
        private final boolean autoSelectable;
        
        private int recentSelection = -1;
        private int selection = -1;
        
        public Toolbar(List<BufferedImage> resources, boolean horizontal, boolean autoSelectable) {
            this.resources = resources;
            this.horizontal = horizontal;
            this.autoSelectable = autoSelectable;
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, horizontal ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
            for (int i = 0; i < resources.size(); i++) {
                int index = i;
                BufferedImage tile = resources.get(i);
                ToolbarElement image = new ToolbarElement(this, tile, index);
                image.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        prepareSelection();
                        select(index);
                    }
                });
                image.setVisible(true);
                content.add(image);
            }
            setHorizontalScrollBarPolicy(horizontal ? HORIZONTAL_SCROLLBAR_ALWAYS : HORIZONTAL_SCROLLBAR_NEVER);
            setVerticalScrollBarPolicy(horizontal ? VERTICAL_SCROLLBAR_NEVER : VERTICAL_SCROLLBAR_ALWAYS);
            setViewportView(content);
            setVisible(true);
        }
        
        public final int getSelection() {
            return selection;
        }
        
        public final void select(int selection) {
            this.selection = Math.min(resources.size() - 1, selection);
            if (selection >= 0) {
                JScrollBar scrollBar = horizontal ? getHorizontalScrollBar() : getVerticalScrollBar();
                int min = scrollBar.getMinimum();
                int max = scrollBar.getMaximum();
                double ratio = Math.max(0, Math.min(1, (double) selection / ((double) resources.size() - 1)) - 0.1);
                scrollBar.setValue(min + (int) (ratio * (max - min)));
            }
        }
        
        public final void toggleSelection() {
            if (recentSelection == -1) {
                recentSelection = selection;
                select(-1);
            } else {
                select(recentSelection);
                recentSelection = -1;
            }
        }
        
        public final void prepareSelection() {
            if (recentSelection >= 0) {
                toggleSelection();
            }
        }
        
        public final void toggle() {
            this.setVisible(!this.isVisible());
            if (this.isVisible()) {
                Editor.this.add(this, BorderLayout.PAGE_START);
            } else {
                Editor.this.remove(this);
            }
            Editor.this.revalidate();
        }
        
        public class ToolbarElement extends JLabel {
            
            private final Toolbar parent;
            private final BufferedImage image;
            private final int index;
            
            private boolean selected = true;
            private boolean hovered = false;
            
            public ToolbarElement(Toolbar parent, BufferedImage image, int index) {
                this.parent = parent;
                this.image = image;
                this.index = index;
                setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
                setIcon(new ImageIcon(this.image));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                    }
                    
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                    }
                });
            }
            
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (selected) {
                    setIcon(new ImageIcon(this.image));
                    setBorder(new MatteBorder(MARGIN, MARGIN, MARGIN, MARGIN, new Color(inferOrientation && parent.autoSelectable ? 0xdb2137 : 0x4287f5)));
                } else {
                    setIcon(new ImageIcon(hovered ? this.image : transparentImage(this.image, 0.35f)));
                    setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
                }
                repaint();
            }
            
            @Override
            public void repaint() {
                super.repaint();
                selected = (parent != null && parent.selection == index);
            }
            
        }
    }
    
}
