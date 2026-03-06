package com.mycompany.furniturefit.graphics;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.mycompany.furniturefit.model.Design;
import com.mycompany.furniturefit.model.Furniture;
import com.mycompany.furniturefit.model.Room;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * OpenGL 3D canvas using JOGL (GLJPanel + fixed-function GL2 pipeline).
 *
 * Renders the furniture room via real OpenGL calls:
 *  - Perspective projection
 *  - Phong-style Blinn lighting
 *  - Wood-coloured floor quad
 *  - Four wall quads
 *  - Per-furniture coloured cuboids
 *  - Camera orbit via mouse drag, zoom via scroll wheel
 */
public class OpenGLCanvas3D extends GLJPanel implements GLEventListener {

    // ─── Design data ───
    private volatile Design design;
    private volatile Furniture selectedFurniture;
    private volatile boolean roomVisible = true;

    // ─── Camera ───
    private double azimuth   = 30.0;   // degrees around Y axis
    private double elevation = 25.0;   // degrees above ground
    private double distance  = 14.0;   // metres from origin
    private double panX = 0, panZ = 0; // horizontal pan

    // ─── Mouse drag ───
    private int lastMouseX, lastMouseY;
    private boolean rotating, panning;

    // ─── OpenGL helpers ───
    private final GLU glu = new GLU();
    private FPSAnimator animator;

    // ─── Callback ───
    private Runnable onSelectionChanged;

    // ─── Night mode ───
    private volatile boolean nightMode = false;

    // ────────────────────────────────────────────────────────────
    //  Constructor
    // ────────────────────────────────────────────────────────────

    public OpenGLCanvas3D() {
        super(buildCapabilities());
        addGLEventListener(this);
        setBackground(new Color(214, 234, 250));  // light sky blue

        // Mouse listeners for camera control
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                rotating = SwingUtilities.isLeftMouseButton(e) && !e.isAltDown();
                panning  = SwingUtilities.isRightMouseButton(e)
                        || (SwingUtilities.isLeftMouseButton(e) && e.isAltDown());
                requestFocusInWindow();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                if (rotating) {
                    azimuth   -= dx * 0.5;
                    elevation  = Math.max(-89, Math.min(89, elevation + dy * 0.4));
                } else if (panning) {
                    double az = Math.toRadians(azimuth);
                    panX += (dx * Math.cos(az) + dy * Math.sin(az)) * 0.02 * distance * 0.12;
                    panZ += (-dx * Math.sin(az) + dy * Math.cos(az)) * 0.02 * distance * 0.12;
                }
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }
        });

        addMouseWheelListener(e -> {
            distance = Math.max(2.0, Math.min(35.0, distance + e.getWheelRotation() * 0.6));
            repaint();
        });
    }

    private static GLCapabilities buildCapabilities() {
        GLProfile profile = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        caps.setDoubleBuffered(true);
        return caps;
    }

    // ────────────────────────────────────────────────────────────
    //  Public API  (mirrors Canvas3DPanel)
    // ────────────────────────────────────────────────────────────

    public void setDesign(Design d)           { this.design = d;           repaint(); }
    public void setRoomVisible(boolean roomVisible) { this.roomVisible = roomVisible; repaint(); }
    public void setSelectedFurniture(Furniture f) { this.selectedFurniture = f; repaint(); }
    public Furniture getSelectedFurniture()   { return selectedFurniture; }
    public void setOnSelectionChanged(Runnable r) { this.onSelectionChanged = r; }

    public void setNightMode(boolean nightMode) { this.nightMode = nightMode; repaint(); }
    public boolean isNightMode() { return nightMode; }

    public void zoomIn()  { distance = Math.max(2.0,  distance * 0.85); repaint(); }
    public void zoomOut() { distance = Math.min(35.0, distance * 1.18); repaint(); }

    public void startAnimator() {
        if (animator == null) {
            animator = new FPSAnimator(this, 30, true);
            animator.start();
        }
    }

    public void stopAnimator() {
        if (animator != null) { animator.stop(); animator = null; }
    }

    // ────────────────────────────────────────────────────────────
    //  GLEventListener implementation
    // ────────────────────────────────────────────────────────────

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.84f, 0.92f, 0.98f, 1.0f);   // light sky blue
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glShadeModel(GL2.GL_SMOOTH);

        // ── Lighting ──
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        // Ambient
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT,  new float[]{0.60f, 0.60f, 0.60f, 1.0f}, 0);
        // Diffuse (warm sun)
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE,  new float[]{0.90f, 0.88f, 0.82f, 1.0f}, 0);
        // Specular
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{0.40f, 0.40f, 0.40f, 1.0f}, 0);

        // Enable colour material so glColor3f works with lighting
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);

        // Smooth anti-aliasing lines
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

        // MSAA
        gl.glEnable(GL2.GL_MULTISAMPLE);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL2 gl = drawable.getGL().getGL2();
        if (h == 0) h = 1;
        float aspect = (float) w / h;

        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(55.0, aspect, 0.1, 200.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // ── Dynamic background and lighting depending on night mode ──
        if (nightMode) {
            gl.glClearColor(0.04f, 0.05f, 0.12f, 1.0f);  // dark navy
            // Dim ambient for night
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT,  new float[]{0.08f, 0.08f, 0.12f, 1.0f}, 0);
            // Moonlight (cool blue, very dim)
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE,  new float[]{0.12f, 0.12f, 0.18f, 1.0f}, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{0.05f, 0.05f, 0.08f, 1.0f}, 0);
        } else {
            gl.glClearColor(0.84f, 0.92f, 0.98f, 1.0f);  // light sky blue
            // Restore daytime lighting
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT,  new float[]{0.60f, 0.60f, 0.60f, 1.0f}, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE,  new float[]{0.90f, 0.88f, 0.82f, 1.0f}, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{0.40f, 0.40f, 0.40f, 1.0f}, 0);
        }

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // ── Camera position from spherical coordinates ──
        double azRad = Math.toRadians(azimuth);
        double elRad = Math.toRadians(elevation);
        double eyeX = distance * Math.sin(azRad) * Math.cos(elRad) + panX;
        double eyeY = distance * Math.sin(elRad);
        double eyeZ = distance * Math.cos(azRad) * Math.cos(elRad) + panZ;

        glu.gluLookAt(eyeX, eyeY, eyeZ,   // eye
                      panX,  0.0, panZ,   // centre
                      0.0,   1.0, 0.0);   // up

        // ── Sun / moon position (moves with camera so scene is always lit) ──
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION,
                new float[]{ (float)eyeX * 2, (float)(eyeY * 2 + 5), (float)eyeZ * 2, 1.0f }, 0);

        // ── Lamp point lights (night mode only) ──
        disableAllLampLights(gl);
        if (nightMode && design != null) {
            enableLampLights(gl);
        }

        if (design == null) {
            drawGrid(gl);
            drawPlaceholderText(gl);
            return;
        }

        drawGrid(gl);
        if (roomVisible) {
            drawRoom(gl);
            drawFurnitureItems(gl);
        }

        // Draw lamp glow halos in night mode
        if (nightMode && roomVisible) {
            drawLampGlowEffects(gl);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        stopAnimator();
    }

    // ────────────────────────────────────────────────────────────
    //  Scene drawing
    // ────────────────────────────────────────────────────────────

    /** Draws the room floor and walls. */
    private void drawRoom(GL2 gl) {
        if (design == null || design.getRoom() == null) return;
        Room room = design.getRoom();
        float w  = (float) room.getWidth();
        float d  = (float) room.getDepth();
        float h  = (float) room.getHeight();
        float hw = w / 2f, hd = d / 2f;
        boolean lShaped = room.getShape() == Room.Shape.L_SHAPED;

        // ── Floor (use room floorColor, fallback to wood tan) ──
        Color floorCol = room.getFloorColor() != null ? room.getFloorColor() : new Color(190, 148, 86);
        setColor(gl, floorCol);
        gl.glNormal3f(0, 1, 0);

        if (lShaped) {
            // Front half: full width, z from 0 to +hd
            gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-hw, 0,  hd);
                gl.glVertex3f( hw, 0,  hd);
                gl.glVertex3f( hw, 0,   0);
                gl.glVertex3f(-hw, 0,   0);
            gl.glEnd();
            // Back-left: left half, z from -hd to 0
            gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-hw, 0,  0);
                gl.glVertex3f(  0, 0,  0);
                gl.glVertex3f(  0, 0, -hd);
                gl.glVertex3f(-hw, 0, -hd);
            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-hw, 0,  hd);
                gl.glVertex3f( hw, 0,  hd);
                gl.glVertex3f( hw, 0, -hd);
                gl.glVertex3f(-hw, 0, -hd);
            gl.glEnd();
        }

        // Floor grid lines
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4f(
            floorCol.getRed()   / 255f * 0.70f,
            floorCol.getGreen() / 255f * 0.70f,
            floorCol.getBlue()  / 255f * 0.70f,
            0.45f);
        gl.glLineWidth(0.5f);
        gl.glBegin(GL2.GL_LINES);
        if (lShaped) {
            // Front half grid
            for (float gx = -hw; gx <= hw; gx += 0.5f) {
                gl.glVertex3f(gx, 0.001f,  hd); gl.glVertex3f(gx, 0.001f,  0);
            }
            for (float gz = 0; gz <= hd; gz += 0.5f) {
                gl.glVertex3f(-hw, 0.001f, gz); gl.glVertex3f(hw, 0.001f, gz);
            }
            // Back-left grid
            for (float gx = -hw; gx <= 0; gx += 0.5f) {
                gl.glVertex3f(gx, 0.001f,  0); gl.glVertex3f(gx, 0.001f, -hd);
            }
            for (float gz = -hd; gz <= 0; gz += 0.5f) {
                gl.glVertex3f(-hw, 0.001f, gz); gl.glVertex3f(0, 0.001f, gz);
            }
        } else {
            for (float gx = -hw; gx <= hw; gx += 0.5f) {
                gl.glVertex3f(gx, 0.001f,  hd); gl.glVertex3f(gx, 0.001f, -hd);
            }
            for (float gz = -hd; gz <= hd; gz += 0.5f) {
                gl.glVertex3f(-hw, 0.001f, gz); gl.glVertex3f( hw, 0.001f, gz);
            }
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_LIGHTING);

        // ── Walls (use room wallColor) ──
        Color wallCol = room.getWallColor() != null ? room.getWallColor() : new Color(150, 78, 48);

        if (lShaped) {
            drawLShapedWalls(gl, hw, hd, h, wallCol);
        } else {
            // Back wall  (z = -hd, normal faces +Z toward viewer)
            setColor(gl, wallCol);
            drawWallQuad(gl, -hw, 0, -hd,  hw, 0, -hd,  hw, h, -hd,  -hw, h, -hd,  0, 0, 1);

            // Left wall  (x = -hw)
            setColor(gl, RenderUtils.darken(wallCol, 0.88));
            drawWallQuad(gl, -hw, 0,  hd,  -hw, 0, -hd,  -hw, h, -hd,  -hw, h,  hd,  1, 0, 0);

            // Right wall  (x = +hw)
            setColor(gl, RenderUtils.darken(wallCol, 0.82));
            drawWallQuad(gl,  hw, 0, -hd,   hw, 0,  hd,   hw, h,  hd,   hw, h, -hd,  -1, 0, 0);
            // Front wall intentionally omitted to keep interior visible
        }
    }

    /**
     * Draws an L-shaped room walls only: the back-right quadrant (x>0, z<0) is removed.
     * Floor is already drawn in drawRoom(). No front wall (open for viewing).
     */
    private void drawLShapedWalls(GL2 gl, float hw, float hd, float h, Color wallCol) {
        // Left wall — full depth
        setColor(gl, RenderUtils.darken(wallCol, 0.88));
        drawWallQuad(gl, -hw, 0,  hd, -hw, 0, -hd, -hw, h, -hd, -hw, h,  hd,  1, 0, 0);
        // Back wall — left half only
        setColor(gl, wallCol);
        drawWallQuad(gl, -hw, 0, -hd,   0, 0, -hd,   0, h, -hd, -hw, h, -hd,  0, 0, 1);
        // Inner step vertical (x=0, z from -hd to 0, normal faces right/+x inside)
        setColor(gl, RenderUtils.darken(wallCol, 0.85));
        drawWallQuad(gl,   0, 0, -hd,   0, 0,  0,    0, h,  0,    0, h, -hd, -1, 0, 0);
        // Inner step horizontal (z=0, x from 0 to +hw, normal faces front/+z inside)
        setColor(gl, RenderUtils.darken(wallCol, 0.82));
        drawWallQuad(gl,   0, 0,  0,   hw, 0,  0,   hw, h,  0,    0, h,  0,   0, 0, 1);
        // Right wall — front half only
        setColor(gl, RenderUtils.darken(wallCol, 0.82));
        drawWallQuad(gl,  hw, 0,  0,   hw, 0,  hd,  hw, h,  hd,  hw, h,  0,  -1, 0, 0);
    }

    /** Helper to draw a single wall quad with a given normal. */
    private void drawWallQuad(GL2 gl,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz) {
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(nx, ny, nz);
            gl.glVertex3f(x1, y1, z1);
            gl.glVertex3f(x2, y2, z2);
            gl.glVertex3f(x3, y3, z3);
            gl.glVertex3f(x4, y4, z4);
        gl.glEnd();
    }

    // ────────────────────────────────────────────────────────────
    //  Furniture — type-specific 3-D models
    // ────────────────────────────────────────────────────────────

    /** Draws all furniture items with type-specific geometry. */
    private void drawFurnitureItems(GL2 gl) {
        if (design == null) return;
        for (Furniture f : design.getFurnitureList()) {
            boolean sel = f == selectedFurniture;
            float fx  = (float) f.getX();
            float fz  = (float) f.getY();
            float rot = (float) f.getRotation();
            float fw  = (float) f.getWidth();
            float fd  = (float) f.getDepth();
            float fh  = (float) f.getHeight();

            Color base = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
            if (sel) base = new Color(
                    Math.min(255, base.getRed()   + 60),
                    Math.min(255, base.getGreen() + 110),
                    Math.min(255, base.getBlue()  + 60));

            gl.glPushMatrix();
            gl.glTranslatef(fx, 0, fz);
            gl.glRotatef(-rot, 0, 1, 0);

            Color lightCol = f.getLightColor() != null ? f.getLightColor() : new Color(255,240,200);
            switch (f.getType()) {
                case CHAIR            -> drawChairModel       (gl, fw, fd, fh, base);
                case DINING_TABLE     -> drawTableModel       (gl, fw, fd, fh, base);
                case SIDE_TABLE       -> drawSideTableModel   (gl, fw, fd, fh, base);
                case COFFEE_TABLE     -> drawCoffeeTableModel (gl, fw, fd, fh, base);
                case SOFA             -> drawSofaModel        (gl, fw, fd, fh, base);
                case BED              -> drawBedModel         (gl, fw, fd, fh, base);
                case SHELF            -> drawShelfModel       (gl, fw, fd, fh, base);
                case WARDROBE         -> drawWardrobeModel    (gl, fw, fd, fh, base);
                case DESK             -> drawDeskModel        (gl, fw, fd, fh, base);
                case LAMP             -> drawLampModel        (gl, fw, fd, fh, base);
                case PENDANT_LIGHT    -> drawPendantLightModel(gl, fw, fd, fh, base, lightCol, f.isLightOn());
                case FLOOR_LAMP_LIGHT -> drawFloorLampLightModel(gl, fw, fd, fh, base, lightCol, f.isLightOn());
                case CEILING_LIGHT    -> drawCeilingLightModel(gl, fw, fd, fh, base, lightCol, f.isLightOn());
                case WALL_LIGHT       -> drawWallLightModel   (gl, fw, fd, fh, base, lightCol, f.isLightOn());
                case SPOTLIGHT        -> drawSpotlightModel   (gl, fw, fd, fh, base, lightCol, f.isLightOn());
                case TABLE_LAMP_LIGHT -> drawTableLampModel   (gl, fw, fd, fh, base, lightCol, f.isLightOn());
            }

            // Green selection outline around bounding box
            if (sel) {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glColor4f(0.16f, 0.71f, 0.30f, 0.95f);
                gl.glLineWidth(2.5f);
                drawBBoxEdges(gl, fw / 2f, fh, fd / 2f);
                gl.glEnable(GL2.GL_LIGHTING);
            }

            gl.glPopMatrix();
        }
    }

    // ── Low-level helpers ────────────────────────────────────────

    /**
     * Draws a solid box with lower-back-left corner at (ox, oy, oz)
     * and dimensions (w, d, h).  Faces are automatically shaded.
     * Call inside a glPushMatrix block; lighting must be enabled.
     */
    private void glBox(GL2 gl,
                       float ox, float oy, float oz,
                       float w,  float d,  float h,
                       Color col) {
        float x0 = ox,     x1 = ox + w;
        float y0 = oy,     y1 = oy + h;
        float z0 = oz - d, z1 = oz;       // z grows toward viewer

        // Top (brightest)
        setColor(gl, RenderUtils.lighten(col, 0.22f));
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0, 1, 0);
            gl.glVertex3f(x0, y1, z1); gl.glVertex3f(x1, y1, z1);
            gl.glVertex3f(x1, y1, z0); gl.glVertex3f(x0, y1, z0);
        gl.glEnd();
        // Front
        setColor(gl, col);
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0, 0, 1);
            gl.glVertex3f(x0, y0, z1); gl.glVertex3f(x1, y0, z1);
            gl.glVertex3f(x1, y1, z1); gl.glVertex3f(x0, y1, z1);
        gl.glEnd();
        // Back
        setColor(gl, RenderUtils.darken(col, 0.80f));
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0, 0, -1);
            gl.glVertex3f(x1, y0, z0); gl.glVertex3f(x0, y0, z0);
            gl.glVertex3f(x0, y1, z0); gl.glVertex3f(x1, y1, z0);
        gl.glEnd();
        // Left
        setColor(gl, RenderUtils.darken(col, 0.87f));
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(-1, 0, 0);
            gl.glVertex3f(x0, y0, z0); gl.glVertex3f(x0, y0, z1);
            gl.glVertex3f(x0, y1, z1); gl.glVertex3f(x0, y1, z0);
        gl.glEnd();
        // Right
        setColor(gl, RenderUtils.darken(col, 0.92f));
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(1, 0, 0);
            gl.glVertex3f(x1, y0, z1); gl.glVertex3f(x1, y0, z0);
            gl.glVertex3f(x1, y1, z0); gl.glVertex3f(x1, y1, z1);
        gl.glEnd();
        // Bottom (rarely visible — darkest)
        setColor(gl, RenderUtils.darken(col, 0.60f));
        gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0, -1, 0);
            gl.glVertex3f(x0, y0, z1); gl.glVertex3f(x0, y0, z0);
            gl.glVertex3f(x1, y0, z0); gl.glVertex3f(x1, y0, z1);
        gl.glEnd();
    }

    /**
     * Convenience: centred box — footprint centre at (cx, cz), base at y=oy.
     * w=width along X, d=depth along Z, h=height along Y.
     */
    private void glBoxC(GL2 gl,
                        float cx, float oy, float cz,
                        float w,  float d,  float h,
                        Color col) {
        // glBox uses corner (ox, oy, oz) where z goes from oz-d to oz
        // so centre_z = oz - d/2  => oz = cz + d/2
        glBox(gl, cx - w / 2f, oy, cz + d / 2f, w, d, h, col);
    }

    /** Selection bounding-box edge lines — half-extents hw/hd, full height fh. */
    private void drawBBoxEdges(GL2 gl, float hw, float fh, float hd) {
        gl.glBegin(GL2.GL_LINES);
        float[] xs = {-hw, hw}, zs = {-hd, hd};
        for (float x : xs) for (float z : zs) {
            gl.glVertex3f(x, 0,  z); gl.glVertex3f(x, fh, z);
        }
        for (float y : new float[]{0, fh}) {
            gl.glVertex3f(-hw, y, -hd); gl.glVertex3f( hw, y, -hd);
            gl.glVertex3f( hw, y, -hd); gl.glVertex3f( hw, y,  hd);
            gl.glVertex3f( hw, y,  hd); gl.glVertex3f(-hw, y,  hd);
            gl.glVertex3f(-hw, y,  hd); gl.glVertex3f(-hw, y, -hd);
        }
        gl.glEnd();
    }

    // ── CHAIR ───────────────────────────────────────────────────
    // 4 round legs, flat seat, backrest panel
    private void drawChairModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color wood  = RenderUtils.darken(col, 0.85f);
        Color seat  = RenderUtils.lighten(col, 0.10f);
        float lw    = Math.min(fw, fd) * 0.10f;   // leg width
        float seatY = fh * 0.44f;                  // seat top height
        float seatT = fh * 0.07f;                  // seat thickness
        float hw    = fw / 2f, hd = fd / 2f;

        // 4 legs (front-left, front-right, back-left, back-right)
        float inset = lw * 0.6f;
        glBoxC(gl, -hw + inset, 0, -hd + inset, lw, lw, seatY, wood);  // back-left
        glBoxC(gl,  hw - inset, 0, -hd + inset, lw, lw, seatY, wood);  // back-right
        glBoxC(gl, -hw + inset, 0,  hd - inset, lw, lw, seatY, wood);  // front-left
        glBoxC(gl,  hw - inset, 0,  hd - inset, lw, lw, seatY, wood);  // front-right

        // Seat cushion
        glBoxC(gl, 0, seatY, 0, fw, fd, seatT, seat);

        // Backrest (along the back edge, taller than seat)
        float backH  = fh - seatY - seatT;
        float backT  = fd * 0.12f;
        glBoxC(gl, 0, seatY + seatT, -hd + backT / 2f, fw, backT, backH, wood);

        // Two tall back legs continue above seat
        glBoxC(gl, -hw + inset, seatY + seatT, -hd + inset, lw, lw, backH * 0.92f, wood);
        glBoxC(gl,  hw - inset, seatY + seatT, -hd + inset, lw, lw, backH * 0.92f, wood);
    }

    // ── DINING TABLE ────────────────────────────────────────────
    private void drawTableModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color wood = RenderUtils.darken(col, 0.80f);
        float topT = fh * 0.07f;    // tabletop thickness
        float legH = fh - topT;
        float lw   = Math.min(fw, fd) * 0.07f;
        float hw = fw / 2f, hd = fd / 2f;
        float inset = lw;

        // 4 legs
        glBoxC(gl, -hw + inset, 0, -hd + inset, lw, lw, legH, wood);
        glBoxC(gl,  hw - inset, 0, -hd + inset, lw, lw, legH, wood);
        glBoxC(gl, -hw + inset, 0,  hd - inset, lw, lw, legH, wood);
        glBoxC(gl,  hw - inset, 0,  hd - inset, lw, lw, legH, wood);

        // Tabletop (slightly wider than legs span)
        glBoxC(gl, 0, legH, 0, fw, fd, topT, RenderUtils.lighten(col, 0.15f));
    }

    // ── SIDE TABLE ──────────────────────────────────────────────
    private void drawSideTableModel(GL2 gl, float fw, float fd, float fh, Color col) {
        // Same as dining table but often has a shelf mid-way
        drawTableModel(gl, fw, fd, fh, col);
        // Mid shelf
        float shelfY = fh * 0.38f;
        float shelfT = fh * 0.05f;
        float lw = Math.min(fw, fd) * 0.07f;
        glBoxC(gl, 0, shelfY, 0, fw - lw * 2, fd - lw * 2, shelfT,
               RenderUtils.darken(col, 0.88f));
    }

    // ── COFFEE TABLE ────────────────────────────────────────────
    private void drawCoffeeTableModel(GL2 gl, float fw, float fd, float fh, Color col) {
        // Low table — four angled-ish legs + glass/wood top
        Color wood  = RenderUtils.darken(col, 0.78f);
        Color top   = RenderUtils.lighten(col, 0.20f);
        float topT  = fh * 0.08f;
        float legH  = fh - topT;
        float lw    = Math.min(fw, fd) * 0.08f;
        float hw = fw / 2f, hd = fd / 2f;

        glBoxC(gl, -hw + lw, 0, -hd + lw, lw, lw, legH, wood);
        glBoxC(gl,  hw - lw, 0, -hd + lw, lw, lw, legH, wood);
        glBoxC(gl, -hw + lw, 0,  hd - lw, lw, lw, legH, wood);
        glBoxC(gl,  hw - lw, 0,  hd - lw, lw, lw, legH, wood);
        glBoxC(gl, 0, legH, 0, fw, fd, topT, top);
    }

    // ── SOFA ────────────────────────────────────────────────────
    private void drawSofaModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color fabric  = col;
        Color darker  = RenderUtils.darken(col, 0.82f);
        Color arm     = RenderUtils.darken(col, 0.88f);
        float baseH   = fh * 0.40f;   // seat cushion top
        float armW    = fd * 0.18f;
        float backT   = fd * 0.22f;
        float backH   = fh - baseH;

        // Seat base / cushion
        glBoxC(gl, 0, 0, 0, fw, fd, baseH, fabric);

        // Back cushion (along back edge)
        glBoxC(gl, 0, baseH, -fd / 2f + backT / 2f, fw, backT, backH, darker);

        // Left armrest
        glBoxC(gl, -fw / 2f + armW / 2f, baseH, 0, armW, fd, fh * 0.28f, arm);

        // Right armrest
        glBoxC(gl, fw / 2f - armW / 2f, baseH, 0, armW, fd, fh * 0.28f, arm);

        // Seat cushion dividers (2 pillows visual)
        float divW   = fw * 0.02f;
        Color divide = RenderUtils.darken(col, 0.70f);
        glBoxC(gl, 0, baseH, 0, divW, fd * 0.75f, fh * 0.07f, divide);
    }

    // ── BED ─────────────────────────────────────────────────────
    private void drawBedModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color frame     = RenderUtils.darken(col, 0.72f);
        Color mattress  = new Color(235, 235, 245);
        Color sheet     = new Color(255, 255, 255);
        Color pillow    = new Color(245, 242, 235);
        Color headboard = RenderUtils.darken(col, 0.65f);

        float frameH  = fh * 0.28f;
        float mattH   = fh * 0.22f;
        float headH   = fh * 0.75f;
        float headT   = fd * 0.08f;
        float footH   = fh * 0.38f;
        float footT   = fd * 0.06f;

        // Bed frame
        glBoxC(gl, 0, 0, 0, fw, fd, frameH, frame);

        // Mattress
        float margin = fw * 0.03f;
        glBoxC(gl, 0, frameH, 0, fw - margin * 2, fd - headT, mattH, mattress);

        // White sheet / cover (slightly raised on top of mattress)
        glBoxC(gl, 0, frameH + mattH, 0,
               fw - margin * 2, (fd - headT) * 0.65f, fh * 0.04f, sheet);

        // Headboard (tall back panel)
        glBoxC(gl, 0, 0, -fd / 2f + headT / 2f, fw, headT, headH, headboard);

        // Footboard (short front panel)
        glBoxC(gl, 0, 0,  fd / 2f - footT / 2f, fw, footT, footH, frame);

        // Two pillows
        float pillowW = fw * 0.38f;
        float pillowD = fd * 0.18f;
        float pillowH = fh * 0.07f;
        float pillowY = frameH + mattH + fh * 0.04f;
        glBoxC(gl, -fw * 0.22f, pillowY, -fd * 0.28f, pillowW, pillowD, pillowH, pillow);
        glBoxC(gl,  fw * 0.22f, pillowY, -fd * 0.28f, pillowW, pillowD, pillowH, pillow);
    }

    // ── SHELF ───────────────────────────────────────────────────
    private void drawShelfModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color wood  = col;
        Color side  = RenderUtils.darken(col, 0.85f);
        float sideW = fw * 0.055f;
        float shT   = fh * 0.04f;   // shelf board thickness
        int   nSh   = 4;             // number of shelves
        float backT = fd * 0.06f;

        // Left side panel
        glBox(gl, -fw / 2f, 0, fd / 2f, sideW, fd, fh, side);
        // Right side panel
        glBox(gl,  fw / 2f - sideW, 0, fd / 2f, sideW, fd, fh, side);
        // Back panel (thin)
        glBox(gl, -fw / 2f + sideW, 0, -fd / 2f + backT, fw - sideW * 2, backT, fh,
              RenderUtils.darken(col, 0.78f));

        // Horizontal shelf boards
        for (int i = 0; i <= nSh; i++) {
            float sy = (fh / nSh) * i;
            glBox(gl, -fw / 2f + sideW, sy, fd / 2f,
                  fw - sideW * 2, fd - backT, shT, RenderUtils.lighten(wood, 0.12f));
        }
    }

    // ── WARDROBE ────────────────────────────────────────────────
    private void drawWardrobeModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color body  = col;
        Color door  = RenderUtils.lighten(col, 0.10f);
        Color trim  = RenderUtils.darken(col, 0.65f);
        float panelT = fw * 0.02f;
        float doorW  = fw / 2f - panelT;

        // Main body
        glBoxC(gl, 0, 0, 0, fw, fd, fh, body);

        // Left door panel (slightly raised to give depth illusion)
        float doorInset = fd * 0.07f;
        glBoxC(gl, -fw / 4f, fh * 0.05f, 0,
               doorW, fd - doorInset, fh * 0.88f, door);
        // Right door panel
        glBoxC(gl,  fw / 4f, fh * 0.05f, 0,
               doorW, fd - doorInset, fh * 0.88f, door);

        // Centre divider trim
        glBoxC(gl, 0, 0, fd / 2f - panelT / 2f, panelT, fd, fh, trim);

        // Door handles (small knobs)
        Color knob = new Color(200, 180, 100);
        float knobSz = Math.min(fw, fd) * 0.04f;
        float knobY  = fh * 0.48f;
        float knobZ  = fd / 2f + 0.005f;
        glBoxC(gl, -panelT - knobSz, knobY, knobZ, knobSz, knobSz * 0.5f, knobSz, knob);
        glBoxC(gl,  panelT,          knobY, knobZ, knobSz, knobSz * 0.5f, knobSz, knob);
    }

    // ── DESK ────────────────────────────────────────────────────
    private void drawDeskModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color wood  = col;
        Color panel = RenderUtils.darken(col, 0.82f);
        float topT  = fh * 0.06f;
        float legH  = fh - topT;
        float lw    = Math.min(fw, fd) * 0.07f;
        float hw    = fw / 2f, hd = fd / 2f;

        // 4 legs
        glBoxC(gl, -hw + lw, 0, -hd + lw, lw, lw, legH, RenderUtils.darken(col, 0.78f));
        glBoxC(gl,  hw - lw, 0, -hd + lw, lw, lw, legH, RenderUtils.darken(col, 0.78f));
        glBoxC(gl, -hw + lw, 0,  hd - lw, lw, lw, legH, RenderUtils.darken(col, 0.78f));
        glBoxC(gl,  hw - lw, 0,  hd - lw, lw, lw, legH, RenderUtils.darken(col, 0.78f));

        // Modesty panel (privacy/back panel under desk)
        float panelH = legH * 0.55f;
        glBoxC(gl, 0, 0, -hd + fd * 0.04f, fw - lw * 2, fd * 0.06f, panelH, panel);

        // Tabletop
        glBoxC(gl, 0, legH, 0, fw, fd, topT, RenderUtils.lighten(wood, 0.18f));
    }

    // ── LAMP ────────────────────────────────────────────────────
    private void drawLampModel(GL2 gl, float fw, float fd, float fh, Color col) {
        Color base   = RenderUtils.darken(col, 0.60f);
        Color pole   = RenderUtils.darken(col, 0.55f);
        Color shade  = new Color(255, 240, 200);
        Color glow   = new Color(255, 255, 180, 200);

        float baseW  = fw * 0.55f;
        float baseH  = fh * 0.06f;
        float poleW  = fw * 0.08f;
        float shadeW = fw * 0.85f;
        float shadeH = fh * 0.22f;
        float poleH  = fh - baseH - shadeH;

        // Base disc (flattened box)
        glBoxC(gl, 0, 0, 0, baseW, baseW, baseH, base);

        // Pole
        glBoxC(gl, 0, baseH, 0, poleW, poleW, poleH, pole);

        // Shade (frustum approximated as box — wider than pole)
        glBoxC(gl, 0, baseH + poleH, 0, shadeW, shadeW, shadeH, shade);

        // Glow disc on underside of shade
        gl.glDisable(GL2.GL_LIGHTING);
        setColor(gl, glow);
        float gw = shadeW * 0.5f;
        float gz = 0;
        float gy = baseH + poleH + 0.01f;
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex3f(0, gy, 0);
            int segs = 16;
            for (int i = 0; i <= segs; i++) {
                double a = 2 * Math.PI * i / segs;
                gl.glVertex3f((float)(Math.cos(a) * gw), gy, (float)(Math.sin(a) * gw));
            }
        gl.glEnd();
        gl.glEnable(GL2.GL_LIGHTING);
    }

    // ── LIGHT FIXTURE 3D MODELS ─────────────────────────────────

    private void drawCylinder(GL2 gl, float cx, float baseY, float cz, float radius, float height, int segs, Color col) {
        setColor(gl, col);
        for (int i = 0; i < segs; i++) {
            double a0 = 2*Math.PI*i/segs, a1 = 2*Math.PI*(i+1)/segs;
            float x0=cx+(float)(Math.cos(a0)*radius), z0=cz+(float)(Math.sin(a0)*radius);
            float x1=cx+(float)(Math.cos(a1)*radius), z1=cz+(float)(Math.sin(a1)*radius);
            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f((float)Math.cos((a0+a1)/2), 0, (float)Math.sin((a0+a1)/2));
            gl.glVertex3f(x0,baseY,z0); gl.glVertex3f(x1,baseY,z1);
            gl.glVertex3f(x1,baseY+height,z1); gl.glVertex3f(x0,baseY+height,z0);
            gl.glEnd();
        }
        setColor(gl, RenderUtils.lighten(col,0.15f));
        gl.glBegin(GL2.GL_TRIANGLE_FAN); gl.glNormal3f(0,1,0); gl.glVertex3f(cx,baseY+height,cz);
        for (int i=0;i<=segs;i++) { double a=2*Math.PI*i/segs; gl.glVertex3f(cx+(float)(Math.cos(a)*radius),baseY+height,cz+(float)(Math.sin(a)*radius)); }
        gl.glEnd();
    }

    private void drawCone(GL2 gl, float cx, float baseY, float cz, float botR, float topR, float h, int segs, Color col) {
        setColor(gl, col);
        for (int i = 0; i < segs; i++) {
            double a0=2*Math.PI*i/segs, a1=2*Math.PI*(i+1)/segs;
            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f((float)Math.cos((a0+a1)/2), 0.3f, (float)Math.sin((a0+a1)/2));
            gl.glVertex3f(cx+(float)(Math.cos(a0)*botR),baseY,cz+(float)(Math.sin(a0)*botR));
            gl.glVertex3f(cx+(float)(Math.cos(a1)*botR),baseY,cz+(float)(Math.sin(a1)*botR));
            gl.glVertex3f(cx+(float)(Math.cos(a1)*topR),baseY+h,cz+(float)(Math.sin(a1)*topR));
            gl.glVertex3f(cx+(float)(Math.cos(a0)*topR),baseY+h,cz+(float)(Math.sin(a0)*topR));
            gl.glEnd();
        }
    }

    private void drawLightGlow(GL2 gl, float cx, float cy, float cz, float radius, Color lc, boolean on) {
        if (!on) return;
        gl.glDisable(GL2.GL_LIGHTING); gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE); gl.glDepthMask(false);
        float r=lc.getRed()/255f, g=lc.getGreen()/255f, b=lc.getBlue()/255f;
        drawGlowDisc(gl, cx, cy, cz, radius, new float[]{r,g,b,0.45f});
        drawGlowDisc(gl, cx, 0.005f, cz, radius*3f, new float[]{r,g,b,0.12f});
        gl.glDepthMask(true); gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL2.GL_BLEND); gl.glEnable(GL2.GL_LIGHTING);
    }

    private void drawPendantLightModel(GL2 gl, float fw, float fd, float fh, Color col, Color lc, boolean on) {
        float rH = design!=null?(float)design.getRoom().getHeight():3f;
        drawCylinder(gl, 0, fh, 0, 0.01f, rH-fh-0.02f, 8, RenderUtils.darken(col,0.5f));
        drawCone(gl, 0, 0, 0, fw*0.5f, fw*0.12f, fh*0.6f, 16, col);
        drawCylinder(gl, 0, -fh*0.15f, 0, fw*0.15f, fh*0.2f, 12, on?lc:new Color(200,200,190));
        drawLightGlow(gl, 0, 0, 0, fw*0.7f, lc, on);
    }

    private void drawFloorLampLightModel(GL2 gl, float fw, float fd, float fh, Color col, Color lc, boolean on) {
        Color dk=RenderUtils.darken(col,0.4f);
        drawCylinder(gl, 0, 0, 0, fw*0.45f, fh*0.04f, 16, dk);
        drawCylinder(gl, 0, fh*0.04f, 0, fw*0.04f, fh*0.68f, 10, dk);
        drawCone(gl, 0, fh*0.72f, 0, fw*0.5f, fw*0.2f, fh*0.25f, 16, RenderUtils.lighten(col,0.3f));
        drawCylinder(gl, 0, fh*0.72f, 0, fw*0.08f, fh*0.08f, 10, on?lc:new Color(200,200,190));
        drawLightGlow(gl, 0, fh*0.75f, 0, fw*0.5f, lc, on);
    }

    private void drawCeilingLightModel(GL2 gl, float fw, float fd, float fh, Color col, Color lc, boolean on) {
        float rH = design!=null?(float)design.getRoom().getHeight():3f;
        drawCylinder(gl, 0, rH-fh, 0, fw*0.5f, fh*0.4f, 20, col);
        drawCylinder(gl, 0, rH-fh-fh*0.15f, 0, fw*0.45f, fh*0.15f, 20, on?lc:new Color(230,230,235));
        drawLightGlow(gl, 0, rH-fh-fh*0.1f, 0, fw*0.6f, lc, on);
    }

    private void drawWallLightModel(GL2 gl, float fw, float fd, float fh, Color col, Color lc, boolean on) {
        Color bk=RenderUtils.darken(col,0.6f);
        glBoxC(gl, 0, fh*0.4f, -fd*0.4f, fw*0.3f, fd*0.08f, fh*0.3f, bk);
        drawCone(gl, 0, fh*0.3f, fd*0.15f, fw*0.4f, fw*0.15f, fh*0.5f, 14, col);
        drawCylinder(gl, 0, fh*0.32f, fd*0.15f, fw*0.1f, fh*0.12f, 10, on?lc:new Color(200,200,190));
        drawLightGlow(gl, 0, fh*0.4f, fd*0.2f, fw*0.4f, lc, on);
    }

    private void drawSpotlightModel(GL2 gl, float fw, float fd, float fh, Color col, Color lc, boolean on) {
        float rH = design!=null?(float)design.getRoom().getHeight():3f;
        glBoxC(gl, 0, rH-0.03f, 0, fw*2f, fd*0.5f, 0.03f, RenderUtils.darken(col,0.3f));
        drawCone(gl, 0, rH-fh, 0, fw*0.15f, fw*0.4f, fh*0.7f, 14, col);
        drawCylinder(gl, 0, rH-fh-0.01f, 0, fw*0.1f, fh*0.08f, 10, on?lc:new Color(180,180,180));
        drawLightGlow(gl, 0, rH-fh, 0, fw*0.35f, lc, on);
    }

    private void drawTableLampModel(GL2 gl, float fw, float fd, float fh, Color col, Color lc, boolean on) {
        Color dk=RenderUtils.darken(col,0.5f);
        drawCylinder(gl, 0, 0, 0, fw*0.35f, fh*0.08f, 14, dk);
        drawCone(gl, 0, fh*0.08f, 0, fw*0.3f, fw*0.15f, fh*0.2f, 14, col);
        drawCylinder(gl, 0, fh*0.31f, 0, fw*0.04f, fh*0.2f, 8, dk);
        drawCone(gl, 0, fh*0.5f, 0, fw*0.5f, fw*0.35f, fh*0.45f, 16, RenderUtils.lighten(col,0.35f));
        drawCylinder(gl, 0, fh*0.5f, 0, fw*0.08f, fh*0.1f, 10, on?lc:new Color(200,200,190));
        drawLightGlow(gl, 0, fh*0.55f, 0, fw*0.4f, lc, on);
    }

    /** Large light ground plane + fine grid outside the room. */
    private void drawGrid(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        float ext = 30f;

        // Ground plane fill (very light grey)
        gl.glColor4f(0.92f, 0.93f, 0.94f, 1.0f);
        gl.glBegin(GL2.GL_QUADS);
            gl.glVertex3f(-ext, -0.002f, -ext);
            gl.glVertex3f( ext, -0.002f, -ext);
            gl.glVertex3f( ext, -0.002f,  ext);
            gl.glVertex3f(-ext, -0.002f,  ext);
        gl.glEnd();

        // Fine grey grid lines
        gl.glColor4f(0.75f, 0.78f, 0.82f, 0.50f);
        gl.glLineWidth(0.7f);
        gl.glBegin(GL2.GL_LINES);
        for (float g = -ext; g <= ext; g += 0.5f) {
            gl.glVertex3f(g, 0.001f, -ext);
            gl.glVertex3f(g, 0.001f,  ext);
            gl.glVertex3f(-ext, 0.001f, g);
            gl.glVertex3f( ext, 0.001f, g);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_LIGHTING);
    }

    /** Empty state message (rendered in a Swing overlay since OpenGL text is complex). */
    private void drawPlaceholderText(GL2 gl) {
        // Just clear to background — caller can overlay a Swing label if needed
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }

    // ────────────────────────────────────────────────────────────
    //  Night mode lamp lighting
    // ────────────────────────────────────────────────────────────

    /** Enable OpenGL point lights for each lamp that has lightOn=true. Uses GL_LIGHT1..GL_LIGHT7 (max 7 lamps). */
    private void enableLampLights(GL2 gl) {
        if (design == null) return;
        int lightIdx = 0;
        for (Furniture f : design.getFurnitureList()) {
            if (f.getType().isLightFixture() && f.isLightOn() && lightIdx < 7) {
                int glLight = GL2.GL_LIGHT1 + lightIdx;
                gl.glEnable(glLight);
                float lx = (float) f.getX();
                float ly = (float) f.getHeight() * 0.75f;
                float lz = (float) f.getY();
                gl.glLightfv(glLight, GL2.GL_POSITION, new float[]{lx, ly, lz, 1.0f}, 0);
                Color lc = f.getLightColor() != null ? f.getLightColor() : new Color(255,240,200);
                float lr=lc.getRed()/255f, lg=lc.getGreen()/255f, lb=lc.getBlue()/255f;
                gl.glLightfv(glLight, GL2.GL_AMBIENT,  new float[]{lr*0.15f, lg*0.12f, lb*0.06f, 1f}, 0);
                gl.glLightfv(glLight, GL2.GL_DIFFUSE,  new float[]{lr, lg*0.95f, lb*0.7f, 1f}, 0);
                gl.glLightfv(glLight, GL2.GL_SPECULAR, new float[]{lr*0.6f, lg*0.55f, lb*0.4f, 1f}, 0);
                gl.glLightf(glLight, GL2.GL_CONSTANT_ATTENUATION,  0.6f);
                gl.glLightf(glLight, GL2.GL_LINEAR_ATTENUATION,    0.35f);
                gl.glLightf(glLight, GL2.GL_QUADRATIC_ATTENUATION, 0.15f);
                lightIdx++;
            }
        }
    }

    /** Disable all lamp point lights (GL_LIGHT1..GL_LIGHT7). */
    private void disableAllLampLights(GL2 gl) {
        for (int i = 0; i < 7; i++) {
            gl.glDisable(GL2.GL_LIGHT1 + i);
        }
    }

    /** Draw translucent glow discs under each lit lamp (additive blended). */
    private void drawLampGlowEffects(GL2 gl) {
        if (design == null) return;
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE); // additive blend
        gl.glDepthMask(false);

        for (Furniture f : design.getFurnitureList()) {
            if (f.getType().isLightFixture() && f.isLightOn()) {
                float lx = (float) f.getX();
                float lz = (float) f.getY();
                float topY = (float) f.getHeight() * 0.72f;
                Color lc = f.getLightColor() != null ? f.getLightColor() : new Color(255,240,200);
                float r=lc.getRed()/255f, g=lc.getGreen()/255f, b=lc.getBlue()/255f;

                drawGlowDisc(gl, lx, topY, lz, 0.6f, new float[]{r, g, b, 0.35f});
                drawGlowDisc(gl, lx, 0.005f, lz, 1.8f, new float[]{r, g, b, 0.18f});
                drawGlowDisc(gl, lx, 0.005f, lz, 3.0f, new float[]{r, g, b, 0.06f});
            }
        }

        gl.glDepthMask(true);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    /** Draw a flat translucent glow disc (horizontal circle) at the given position. */
    private void drawGlowDisc(GL2 gl, float cx, float cy, float cz, float radius, float[] rgba) {
        int segs = 32;
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        gl.glVertex3f(cx, cy, cz);
        gl.glColor4f(rgba[0], rgba[1], rgba[2], 0.0f); // fade to transparent at edge
        for (int i = 0; i <= segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            gl.glVertex3f(cx + (float)(Math.cos(a) * radius), cy, cz + (float)(Math.sin(a) * radius));
        }
        gl.glEnd();
    }

    // ────────────────────────────────────────────────────────────
    //  Utility
    // ────────────────────────────────────────────────────────────

    private void setColor(GL2 gl, Color c) {
        gl.glColor4f(c.getRed() / 255f, c.getGreen() / 255f,
                     c.getBlue() / 255f, c.getAlpha() / 255f);
    }
}
