package com.bn.view;

import static com.bn.constant.SourceConstant.*;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector3f;

import com.bn.MatrixState.MatrixState2D;
import com.bn.MatrixState.MatrixState3D;
import com.bn.addRigidBody.BoxRigidBody;
import com.bn.addRigidBody.Camera;
import com.bn.addRigidBody.Car;
import com.bn.addRigidBody.Claw;
import com.bn.addRigidBody.Doll;
import com.bn.addRigidBody.Niu;
import com.bn.addRigidBody.Parrot;
import com.bn.addRigidBody.Phone;
import com.bn.addRigidBody.Robot;
import com.bn.addRigidBody.Tv;
import com.bn.catcherFun.MainActivity;
import com.bn.catcherFun.MySurfaceView;
import com.bn.constant.Constant;
import com.bn.constant.SourceConstant;
import com.bn.object.BN2DObject;
import com.bn.object.TexFloor;
import com.bn.thread.HoleThread;
import com.bn.thread.KeyThread;
import com.bn.thread.MoneyThread;
import com.bn.thread.PhysicsThread;
import com.bn.util.DrawNumber;
import com.bn.util.RigidBodyHelper;
import com.bn.util.SliderHelper;
import com.bn.util.manager.ShaderManager;
import com.bn.util.manager.SoundManager;
import com.bn.util.manager.TextureManager;
import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

import android.annotation.SuppressLint;
import android.opengl.GLES30;
import android.view.MotionEvent;

@SuppressLint("UseSparseArrays")
public class GameView extends BNAbstractView {
    public PhysicsThread pt;//物理线程
    public MySurfaceView viewManager;
    public SliderHelper sliderhelper;
    DrawNumber score;
    public CatchSucceedView catchview;
    public DiscreteDynamicsWorld dynamicsWorld;//世界对象
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;//角度缩放比例
    public float xAngle = 0;
    private float mPreviousX;//上次的触控位置X坐标
    public boolean ismoneyout = false;
    public boolean isGrab = false;//是否开始抓的标志
    public boolean isGrabOver = false;//是否抓完的标志
    public boolean isSuccess = false;
    public int successId = 0;
    public boolean isdown = false;
    CollisionShape boxShapeLR;
    CollisionShape boxShapefb;
    CollisionShape boxShapefloor;

    RigidBody boxRigidBodyL;
    RigidBody boxRigidBodyR;
    RigidBody boxRigidBodyF;
    RigidBody boxRigidBodyB;
    RigidBody boxRigidBodyD;
    CollisionShape planeShape;//共用的平面形
    CollisionShape capsuleShape3;//胶囊形状

    CollisionShape cylinderShape0;
    CollisionShape cylinderShape1;
    CollisionShape cylinderShape2;
    CollisionShape[] csa = new CollisionShape[3];

    PhysicsThread pThread;
    KeyThread KThread;
    MoneyThread mThread;

    TexFloor floor;//纹理矩形1
    TexFloor floor1;//纹理矩形1
    int floorid;
    public Claw claw;
    Doll pig0;
    Doll pig1;
    Niu niurg;
    public Phone phone;
    Tv tvrg;
    public BoxRigidBody holeboxrg;
    public int keyState = 0;
    List<BN2DObject> button = new ArrayList<BN2DObject>();//存放BNObject对象
    public List<BN2DObject> menulist = new ArrayList<BN2DObject>();//存放BNObject对象

    public boolean isMenu = false;
    boolean MGStart = false;//游戏开始换图标志位
    boolean gamestart = false;
    int dance = 0;
    public static boolean isdsMoney = false;//游戏开始投金币绘制完标志位
    boolean isStart = false;//开始绘制掉落的金币标志位
    boolean isSX = false;//点击刷新按钮标志位
    boolean isSXMoney = false;
    int danceSX = 0;
    int SpecialJS = 0;
    BN2DObject bn;

    HoleThread mHoleTh = null;

    public GameView(MySurfaceView viewManager) {
        this.viewManager = viewManager;
        initView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        // 从物理屏幕坐标(左上角为原点,并且宽高是物理屏幕大小 转到 中间为原点,宽高固定为1920x1080)
        float x = Constant.fromRealScreenXToStandardScreenX(e.getX());
        float y = Constant.fromRealScreenYToStandardScreenY(e.getY());

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                if (x > menu_left && x < menu_right && y > menu_top && y < menu_bottom) {
                    isMenu = true;// 显示菜单界面(设置界面 MenuView)
                }
                if (!isSuccess) { // 没有捉到娃娃 判断能否开始游戏
                    if (!isMenu && !isCollection && !ismoneyout) {
                        if (x > MGstart_TOUCH_LEFT_x && x < MGstart_TOUCH_RIGHT_x && // 开始按钮
                                y > MGstart_TOUCH_TOP_y && y < MGstart_TOUCH_BOTTOM_y && !isGrab && !gamestart) {
                            MGStart = true;
                            gamestart = true;
                            if (moneycount < 1) {   // 金币数量是否小于1
                                ismoneyout = true;  // money out 没钱了下一次就不能再按start按钮 除非MoneyThread把金钱增加了
                                isStart = false;    // 不会掉落金币
                                MGStart = false;    // 不能开始游戏  需要等待一分钟 系统自动增加金币
                            } else {
                                moneycount = moneycount - 1; // 金币数量减去1
                                MainActivity.editor.putString("count", Integer.toString(moneycount));
                                MainActivity.editor.commit();// 保存金币数量到.xml
                            }
                        }
                        if (x > shuaxin_TOUCH_LEFT_x && x < shuaxin_TOUCH_RIGHT_x &&  // 刷新按钮
                                y > shuaxin_TOUCH_TOP_y && y < shuaxin_TOUCH_BOTTOM_y && !gamestart) {
                            if (moneycount < 3) {// 金币数量少于3 不可以刷新
                                isSX = false;       // 金币按钮 不显示按下的效果
                                isSXMoney = false;  // 停止绘制 3个金币掉下 的渲染
                            } else {
                                isSX = true;        // 掉落刷新的3个金币 绘制 刷新按钮按下效果
                                                    // 并后续 渲染线程 判断isSX为true后会设置isSXMoney为true,从而绘制3个金币下落
                                                    // isSXMoney 可能在a.绘制金币掉落完成 b.下一次刷新遇到金币少于3个 c.切换到MenuView后返回
                                isupdate = true;    // 更新刚体的刷新方法
                                moneycount = moneycount - 3;
                                MainActivity.editor.putString("count", Integer.toString(moneycount));
                                MainActivity.editor.commit();
                            }
                        }
                        if (isdsMoney) { // 已经投币 并且投币效果渲染完毕
                            if (xAngle > ANGLE_MIN + 10 && xAngle < ANGLE_MAX - 10) {
                                setkeyState(x, y, 0x04, 0x08, 0x02, 0x01);

                            }
                            if (xAngle < ANGLE_MIN + 10) {
                                xAngle = ANGLE_MIN;
                                setkeyState(x, y, 0x02, 0x01, 0x08, 0x04);


                            }
                            if (xAngle > ANGLE_MAX - 10) {
                                setkeyState(x, y, 0x01, 0x02, 0x04, 0x08);

                            }
                        }
                    }

                    // 判断上下左右按钮是否在触摸中 是的话设置isdown=true
                    if (x >= tol_left && x <= tor_left && y >= tou_top && y <= tod_bottom) {
                        isdown = true;
                    }
                } else { // 如果抓到娃娃了 就给到CatchSucceedView处理触摸事件 Gameview不做处理
                    return catchview.onTouchEvent(e);
                }
                break;
            case MotionEvent.ACTION_MOVE: // 处理屏幕滑动事件

                float dx = x - mPreviousX; // 计算触控笔‘X位移’
                if (isCollection && !isSuccess && isdown) {
                    return false; // 如果在收藏界面  上下左右按下/按下了捉娃娃按钮正捉娃娃中  没有绘制成功捉娃娃界面
                }

                System.out.println(keyState);// 不是在上下左右按钮上按下后移动

                if (keyState == 0) {        // 处理摄像机位置变换
                    if (Math.abs(dx) <= 0.04f && isdown) { // 小于阈值 不一定摄像机
                        break;
                    }

                    xAngle -= dx * TOUCH_SCALE_FACTOR;   // 计算摄像机变换的角度

                    if (xAngle < ANGLE_MIN) {            // 摄像机最大最小角度 -90 ~ 90
                        xAngle = ANGLE_MIN;
                    } else if (xAngle > ANGLE_MAX) {
                        xAngle = ANGLE_MAX;
                    }
                    calculateMainAndMirrorCamera(xAngle);// 调用变换摄像机的方法
                }
                break;
            case MotionEvent.ACTION_UP:
                // 恢复 刷新 和 开始 按钮 的标记
                if (x > MGstart_TOUCH_LEFT_x && x < MGstart_TOUCH_RIGHT_x &&
                        y > MGstart_TOUCH_TOP_y && y < MGstart_TOUCH_BOTTOM_y) {

                    MGStart = false;// 松开 开始按钮 绘制没有按下的效果

                }
                if (x > shuaxin_TOUCH_LEFT_x && x < shuaxin_TOUCH_RIGHT_x &&
                        y > shuaxin_TOUCH_TOP_y && y < shuaxin_TOUCH_BOTTOM_y) {
                    isSX = false; // 松开 刷新按钮  绘制没有按下的效果
                }
                keyState = 0; // 没有再按下左右上下按钮了 PhysicsThread不再有效工作
                break;
        }
        mPreviousX = x;//记录触控笔位置
        if (isMenu) {
            return viewManager.menuview.onTouchEvent(e); // 游戏界面--菜单界面 给到菜单界面处理
        }
        return true;
    }

    /*
    * 设置左右上下移动标记,物理线程会读取keyState的状态,按钮取消按下的时候keyState重置为0
    * */
    private void setkeyState(float x, float y, int tolState, int torState, int touState, int todState) {
        if (x >= catch_left && x <= catch_right && y >= catch_top && y <= catch_bottom) {
            isdown = true; // 摄像机位置不能改变
            if (!isGrab) { // 一次夹取没完成
                isGrab = true; // 已经按下捉娃娃按钮，一次夹取完成，正在演示捉娃娃过程中
                allcount++;//总游戏次数
                if(KThread != null){
                    KThread.quitSync();
                }
                KThread = new KeyThread(GameView.this);
                KThread.start(); // 开启夹取娃娃的线程
            }

        }
        if (x >= tol_left && x <= tol_right && y >= tol_top && y <= tol_bottom) {

            if (!isGrab) {

                keyState = tolState;

            }
        }

        if (x >= tor_left && x <= tor_right && y >= tor_top && y <= tor_bottom) {
            if (!isGrab) {

                keyState = torState;
            }

        }

        if (x >= tou_left && x <= tou_right && y >= tou_top && y <= tou_bottom) {

            if (!isGrab) {
                keyState = touState;
            }

        }

        if (x >= tod_left && x <= tod_right && y >= tod_top && y <= tod_bottom) {
            if (!isGrab) {
                keyState = todState;
            }

        }

    }

    @Override
    public void initView() {
        initButton();   // 初始化界面按钮
        initWorld();    // 初始化物理世界
        if (hb != null) {// 模型不为空 启动线程
            mHoleTh = new HoleThread();
            mHoleTh.start();
        }
        update(); // 刷新生成9个娃娃 随机产生
        score = new DrawNumber(this.viewManager); // 创建金币数量对象
        catchview = new CatchSucceedView(this.viewManager); // 成功抓娃娃界面
        pThread = new PhysicsThread(GameView.this); // 物理线程
        mThread = new MoneyThread(GameView.this);   // 金币自动增加线程
        mThread.start();
        pThread.start();
    }

    public void update()//刷新方法
    {
        for (int i = 0; i < 9; i++) { // 随机生成9个娃娃,!Robot是有限制的!
            int random = (int) (Math.random() * 9);
            switch (random) {
                case 0:
                    updatedoll.add(new Niu(
                            niuId,
                            dynamicsWorld,
                            niu,
                            new Vector3f(
                                    dollinitx + i % 4 * spanx,
                                    1,
                                    dollinitz + i / 4 * spanz),
                            0));
                    break;
                case 1:
                    updatedoll.add(new Doll(
                            doll0Id,
                            dynamicsWorld,
                            doll0,
                            new Vector3f(
                                    dollinitx + i % 4 * spanx, 2,
                                    dollinitz + i / 4 * spanz),
                            1));
                    break;
                case 2:
                    updatedoll.add(new Phone(
                            doll2Id,
                            dynamicsWorld,
                            doll2,
                            new Vector3f(dollinitx + i % 4 * spanx, 1.5f, dollinitz + i / 4 * spanz),
                            2));
                    break;
                case 3:
                    updatedoll.add(new Parrot(
                            parrotId,
                            dynamicsWorld,
                            ParrotMd,
                            new Vector3f(dollinitx + i % 4 * spanx, 1, dollinitz + i / 4 * spanz),
                            3));
                    break;
                case 4:
                    if (CollectionView.islock) {
                        updatedoll.add(new Tv(
                                tvId,
                                dynamicsWorld,
                                tvmodle,
                                new Vector3f(dollinitx + i % 4 * spanx, 1, dollinitz + i / 4 * spanz),
                                6));
                    } else {
                        updatedoll.add(new Robot(
                                robotId,
                                dynamicsWorld,
                                RobotMD,
                                new Vector3f(dollinitx + i % 4 * spanx, 1, dollinitz + i / 4 * spanz),
                                6));
                    }
                    break;
                case 5:
                    updatedoll.add(new Car(
                            CarId,
                            dynamicsWorld,
                            CarMD,
                            new Vector3f(dollinitx + i % 4 * spanx, 2, dollinitz + i / 4 * spanz),
                            5));
                    break;
                case 6:
                    updatedoll.add(new Tv(
                            tvId,
                            dynamicsWorld,
                            tvmodle,
                            new Vector3f(dollinitx + i % 4 * spanx, 1, dollinitz + i / 4 * spanz),
                            6));
                    break;
                case 7:
                    updatedoll.add(new Doll(
                            doll1Id,
                            dynamicsWorld,
                            doll1,
                            new Vector3f(dollinitx + i % 4 * spanx, 1.5f, dollinitz + i / 4 * spanz),
                            7));
                    break;
                case 8:
                    updatedoll.add(new Camera(
                            CameraId,
                            dynamicsWorld,
                            Camera,
                            new Vector3f(dollinitx + i % 4 * spanx, 2, dollinitz + i / 4 * spanz),
                            8));
                    break;
            }
        }
    }

    private void initButton() {

        button.add(new BN2DObject(920, 1700, 200, 200, TextureManager.getTextures("catch.png"),
                ShaderManager.getShader(2)));//0

        button.add(new BN2DObject(200, 1600, 150, 150, TextureManager.getTextures("tou.png"),
                ShaderManager.getShader(2)));//1
        button.add(new BN2DObject(200, 1820, 150, 150, TextureManager.getTextures("tod.png"),
                ShaderManager.getShader(2)));//2
        button.add(new BN2DObject(80, 1710, 150, 150, TextureManager.getTextures("tol.png"),
                ShaderManager.getShader(2)));//3
        button.add(new BN2DObject(320, 1710, 150, 150, TextureManager.getTextures("tor.png"),
                ShaderManager.getShader(2)));//4
        button.add(new BN2DObject(Box_x, Box_y, Box_SIZE_x, Box_SIZE_y, Box1Id,
                ShaderManager.getShader(2)));//5收币纸箱的图片
        button.add(new BN2DObject(MGstart_x, MGstart_y, MGstart_SIZE_x, MGstart_SIZE_y, MGstartId,
                ShaderManager.getShader(2)));//6游戏界面中开始游戏的图标
        button.add(new BN2DObject(MGstart_x, MGstart_y, MGstart_SIZE_x, MGstart_SIZE_y, MGstartDownId,
                ShaderManager.getShader(2)));//7游戏界面中的开始游戏按钮按下的图标
        button.add(new BN2DObject(Money_x, Money_y, Money_SIZE_x, Money_SIZE_y, MoneyBoxId,
                ShaderManager.getShader(2)));//8这是游戏界面上面最上面的一个金币的数量的图片
        button.add(new BN2DObject(Money_x, Money_y, Money_SIZE_x, Money_SIZE_y, MoneyId,
                ShaderManager.getShader(2)));//9这是一个游戏界面中落下的金币的图片
        button.add(new BN2DObject(shuaxin_x, shuaxin_y, shuaxin_SIZE_x, shuaxin_SIZE_y, shuaxinId,
                ShaderManager.getShader(2)));//10 这是游戏界面中的刷新按钮
        button.add(new BN2DObject(shuaxin_x, shuaxin_y, shuaxin_SIZE_x, shuaxin_SIZE_y, shuaxinDownId,
                ShaderManager.getShader(2)));//11 这是游戏界面中  刷新按钮按下的图片
        button.add(new BN2DObject(1000, 110, 180, 80, TextureManager.getTextures("menu.png"),
                ShaderManager.getShader(2)));//12
        bn = new BN2DObject(550, 900, 800, 600, TextureManager.getTextures("message.png"),
                ShaderManager.getShader(2));
        button.add(new BN2DObject(Box2_x, Box2_y, Box2_SIZE_x, Box2_SIZE_y, Box2Id,
                ShaderManager.getShader(2)));//13接受金币箱子的上半部
    }

    private void initWorld() {
        //创建碰撞检测配置信息对象
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        //创建碰撞检测算法分配者对象，其功能为扫描所有的碰撞检测对，并确定适用的检测策略对应的算法
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        //设置整个物理世界的边界信息
        Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
        Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);
        int maxProxies = 1024;
        //创建碰撞检测粗测阶段的加速算法对象
        AxisSweep3 overlappingPairCache = new AxisSweep3(worldAabbMin, worldAabbMax, maxProxies);
        //创建推动约束解决者对象
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
        //创建物理世界对象
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver, collisionConfiguration);
        //设置重力加速度
        dynamicsWorld.setGravity(new Vector3f(0, -10, 0));

        cylinderShape0 = new CylinderShape(new Vector3f(2.1f, 2.1f, 2.1f));
        cylinderShape1 = new CylinderShape(new Vector3f(0.7f, 5.6f, 0.7f));
        cylinderShape2 = new CylinderShape(new Vector3f(2.1f, 2.1f, 2.1f));

        csa[0] = cylinderShape0;
        csa[1] = cylinderShape1;
        csa[2] = cylinderShape2;

        //此处后面是画刚体模型
        //创建共用的平面形状
        planeShape = new StaticPlaneShape(new Vector3f(0, 1, 0), 0);
        capsuleShape3 = new CapsuleShape(0.77f * 2, 1.5f * 2);
        boxShapeLR = new BoxShape(new Vector3f(0.05f, 4f, 3.088f));
        boxShapefb = new BoxShape(new Vector3f(3.075f, 4f, 0.05f));
        boxShapefloor = new BoxShape(new Vector3f(3.075f, 0.05f, 3.088f));
        floor = new TexFloor(ShaderManager.getShader(0),
                80 * SourceConstant.UNIT_SIZE, 0, -SourceConstant.UNIT_SIZE, 0, planeShape, dynamicsWorld, 0);

        claw = new Claw(clawId, ganId, dunId, viewManager, dynamicsWorld, bodyForDraws, ShaderManager.getShader(0));

        holeboxrg = new BoxRigidBody(holeboxId, dynamicsWorld, holebox, new Vector3f(holeboxx, holeboxy, holeboxz));
        sliderhelper = new SliderHelper(ganboxId, viewManager, dynamicsWorld);
        boxRigidBodyL = RigidBodyHelper.addRigidBody(0, boxShapeLR, -2.7f, 4, 13, dynamicsWorld, false);
        boxRigidBodyR = RigidBodyHelper.addRigidBody(0, boxShapeLR, 2.7f, 4, 13, dynamicsWorld, false);
        boxRigidBodyF = RigidBodyHelper.addRigidBody(0, boxShapefb, 0f, 3, 10f, dynamicsWorld, false);
        boxRigidBodyB = RigidBodyHelper.addRigidBody(0, boxShapefb, 0f, 3, 16.2f, dynamicsWorld, false);
        boxRigidBodyD = RigidBodyHelper.addRigidBody(0, boxShapefloor, 0f, 0.5f, 13f, dynamicsWorld, false);

    }

    @Override
    public void drawView(GL10 gl) {
        //清除颜色缓存于深度缓存
        GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        if (!isCollection) {
            MatrixState3D.setCamera(
                    EYE_X,   //人眼位置的X
                    EYE_Y,    //人眼位置的Y
                    EYE_Z,   //人眼位置的Z
                    TARGET_X,    //人眼球看的点X
                    TARGET_Y,   //人眼球看的点Y
                    TARGET_Z,   //人眼球看的点Z
                    0,
                    1,
                    0);
        }
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);


        //绘制地板
        MatrixState3D.pushMatrix();
        floor.drawSelf(floorTextureId);
        MatrixState3D.popMatrix();
        MatrixState3D.pushMatrix();
        claw.drawSelf();
        MatrixState3D.popMatrix();
        MatrixState3D.pushMatrix();
        sliderhelper.drawSelf();
        MatrixState3D.popMatrix();


        for (int i = 0; i < updatedoll.size(); i++) {
            updatedoll.get(i).drawSelf();
        }


        holeboxrg.drawSelf();
        drawdoll();       // 绘制场景
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        draw2DObject(gl); // 绘制2D的物体 比如绘制金币数量 drawMoney


        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (isupdate) {//点击刷新按钮
            SpecialJS = 2;
        }
        if (SpecialJS > 1) {
            if (SpecialJS > 30) {
                Special.drawSpecial(5);//播放粒子系统刷新
                if (SpecialJS > 130) {//播放的时间
                    SpecialJS = 0;
                }
            }
            SpecialJS++;
        }

        if (isSuccess) {// 如果成功抓到娃娃 显示 CatchSucceedView 同时触摸事件也给到 CatchSucceedView处理
            catchview.drawView(successId);
            Special.drawSpecial(2);//播放粒子系统刷新
        }
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

    }

    @Override
    public void lostContextOnGLThread() {
        if (pThread != null) {
            pThread.quitSync();
            pThread = null;
        }
        if (KThread != null) {
            KThread.quitSync();
            KThread = null;
        }
        if (mThread != null) {
            mThread.quitSync();
            mThread = null;
        }
        if (mHoleTh != null) {
            mHoleTh.quitSync();
            mHoleTh = null;
        }
    }

    // 绘制场景
    private void drawdoll() {

        MatrixState3D.pushMatrix();
        MatrixState3D.translate(0, -4, 13);
        dollbox.drawSelf(dollboxId);
        MatrixState3D.popMatrix();


        MatrixState3D.pushMatrix();
        MatrixState3D.translate(0, 1, 10);
        hole.drawSelf(holeId);
        MatrixState3D.popMatrix();


        GLES30.glEnable(GLES30.GL_BLEND);// 打开混合
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA); // 设置混合因子
        GLES30.glDisable(GL10.GL_CULL_FACE);
        MatrixState3D.pushMatrix();
        MatrixState3D.translate(1.4f, 0.5f, 14.8f);
        hb.drawSelf(HBId);// 这是出口盒子的玻璃围栏 透明度不断变化 若隐若现 holebox_frag.glsl  HB.png 绿色的一张图片
        MatrixState3D.popMatrix();
        GLES30.glEnable(GL10.GL_CULL_FACE);
        GLES30.glDisable(GLES30.GL_BLEND);

    }

    private void drawMoney() {
        initdatax = 180;
        initdatay = 100;
        score.drawnumber(moneycount, false);
    }

    private void draw2DObject(GL10 gl) {
        if (ismoneyout) {
            bn.drawSelf();
        }
        if (isMenu) {
            viewManager.menuview.drawView(gl);
        }
        if (!isMenu) {
            button.get(12).drawSelf();
            button.get(8).drawSelf();
            drawMoney();
            drawDropMoney();
        }
    }

    // 绘制掉落一个金币
    private void OneMoney() {
        if (dance < 70) {//点击开始按钮后掉落一个金币
            dance++;
            if (dance == 62) {
                if (!effectOff) {
                    SoundManager.instance().playMusic(viewManager.getContext(),SOUND_DropMoney, 0);
                }
            }
            button.get(9).drawSelf(Money_x + dance * 3f, Money_y + dance * 28);
        } else {
            dance++;

            if (dance > 80) {
                isdsMoney = true;//标志着投的一个金币已经掉落到接受金币的箱子里面
            }
        }
    }

    // 绘制掉落三个金币 点击刷新按钮，开始直到掉落进三个金币到箱子里
    private void SanMoney()
    {
        if (danceSX < 70) {
            danceSX++;
            if (danceSX == 62) {
                if (!effectOff) {
                    SoundManager.instance().playMusic(viewManager.getContext(),SOUND_DropMoney, 0);
                }
            }
            button.get(9).drawSelf(Money_x + danceSX * 3f, Money_y + danceSX * 28);
            button.get(9).drawSelf(Money_x + danceSX * 3f, Money_y + danceSX * 30);
            button.get(9).drawSelf(Money_x + danceSX * 3f, Money_y + danceSX * 34);
        } else {
            danceSX++;
            if (danceSX > 80) {
                isSXMoney = false; // 绘制3个金币 掉落完毕
                danceSX = 0;
            }

        }
    }

    private void drawDropMoney() {
        boolean tempLeft = isleft;
        boolean tempRight = isright;
        boolean tempTop = istop;
        boolean tempBottom = isbottom;
        if (xAngle < ANGLE_MIN + 10) {

            tempLeft = istop;
            tempRight = isbottom;
            tempTop = isright;
            tempBottom = isleft;

        }
        if (xAngle > ANGLE_MAX - 10) {


            tempLeft = isbottom;
            tempRight = istop;
            tempTop = isleft;
            tempBottom = isright;
        }
        if (isdsMoney)//点击开始按钮，并绘制完掉落的金币后进入游戏
        {
            MatrixState2D.pushMatrix();//保护现场

            button.get(0).drawSelf();
            if (!tempLeft) {
                button.get(3).drawSelf();
            }
            if (!tempRight) {
                button.get(4).drawSelf();
            }
            if (!tempTop) {
                button.get(1).drawSelf();
            }
            if (!tempBottom) {
                button.get(2).drawSelf();
            }
            MatrixState2D.popMatrix();//恢复现场
        } else {
            //绘制顺序： 先画接受金币的上半部分，后画金币，最后画接受金币的下半部分
            button.get(13).drawSelf();//箱子的上半部分

            if (isStart) {  //  开始按钮  一个金币掉落
                OneMoney();
            }
            if (isSXMoney) {//  刷新按钮  三个个金币掉落 后面会根据触摸事件设置isSX导致isSMMoney=true
                SanMoney();
            }
            button.get(5).drawSelf();//箱子的下半部分

            //刷新与开始游戏俩个按钮的绘制，是否点击
            if (MGStart) {//已经点击游戏开始按钮
                button.get(7).drawSelf();//开始按钮被按下
                isStart = true;//开始按钮被按下，需要绘制掉落的金币

            } else {
                button.get(6).drawSelf();//开始按钮
            }
            if (isSX) {
                button.get(11).drawSelf();//刷新按钮按下
                isSXMoney = true;//开始绘制刷新金币 下一次draw的时候将会 绘制 三个金币掉落
            } else {
                button.get(10).drawSelf();//刷新按钮
            }

        }
    }

    public void reData() {
        isMenu = false;
        MGStart = false;//游戏开始标志位
        dance = 0;
        isdsMoney = false;//游戏开始投金币绘制完标志位
        isStart = false;//开始绘制掉落的金币标志位
        isSX = false;//点击刷新按钮标志位
        isSXMoney = false;
        danceSX = 0;
        isCollection = false;
        isGrab = false;
        gamestart = false;

    }

    // 计算主摄像机观察者的坐标 绕Y轴旋转
    public static void calculateMainAndMirrorCamera(float angle) {
        EYE_X = (float) (r * Math.sin(Math.toRadians(angle)));
        EYE_Y = 4f;
        EYE_Z = (float) (r * Math.cos(Math.toRadians(angle))) + TARGET_Z;
    }
}
