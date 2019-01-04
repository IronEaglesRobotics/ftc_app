package eaglesfe.roverruckus;

import com.eaglesfe.birdseye.FieldPosition;
import com.eaglesfe.birdseye.roverruckus.MineralSample;
import com.eaglesfe.birdseye.roverruckus.RoverRuckusBirdseyeTracker;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE;

import eaglesfe.common.MathHelpers;
import eaglesfe.common.MecanumDrive;

public class Robot {

    private final   HardwareMap                     hardwareMap;
    private         MecanumDrive                    drive;
    private         RoverRuckusBirdseyeTracker      tracker;
    private         DcMotor                         lift;
    private         DcMotor                         collector;
    private         DcMotor                         extend;
    private         Servo                           collectorLeft;
    private         Servo                           collectorRight;
    private         BNO055IMU                       imu;
    private         boolean                         isInitialized;

    public Robot(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;
        initializeHardware();
    }

    /**
     * Initialize all the robot hardware.
     * All devices should be properly confiugred within this method.
     */
    private void initializeHardware() {
        DcMotor frontLeft = this.hardwareMap.dcMotor.get(Constants.FRONT_LEFT);
        frontLeft.setDirection(FORWARD);
        DcMotor frontRight = this.hardwareMap.dcMotor.get(Constants.FRONT_RIGHT);
        frontRight.setDirection(REVERSE);
        DcMotor backLeft = this.hardwareMap.dcMotor.get(Constants.BACK_LEFT);
        backLeft.setDirection(FORWARD);
        DcMotor backRight = this.hardwareMap.dcMotor.get(Constants.BACK_RIGHT);
        backRight.setDirection(REVERSE);
        this.drive = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);

        this.lift = this.hardwareMap.dcMotor.get(Constants.LIFT);
        this.lift.setDirection(FORWARD);
        this.lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        this.lift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        this.collector = this.hardwareMap.dcMotor.get(Constants.ARM);
        this.collector.setDirection(FORWARD);
        this.collector.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        this.collector.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        this.extend = this.hardwareMap.dcMotor.get(Constants.EXTEND);
        this.extend.setDirection(FORWARD);
        this.extend.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.extend.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        this.collectorLeft = this.hardwareMap.servo.get(Constants.COLLECT_LEFT);
        this.collectorLeft.setDirection(Servo.Direction.REVERSE);
        this.collectorLeft.scaleRange(0, .75);
        //this.collectorLeft.setPosition(1);

        this.collectorRight = this.hardwareMap.servo.get(Constants.COLLECT_RIGHT);
        this.collectorRight.setDirection(Servo.Direction.FORWARD);
        this.collectorRight.scaleRange(0, .75);
        //this.collectorRight.setPosition(1);

        this.imu = this.hardwareMap.get(BNO055IMU.class, Constants.GYRO);
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.mode                 = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit            = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit            = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled       = false;
        this.imu.initialize(parameters);

        this.isInitialized = true;
    }

    /** Sets the given motor to isFinished using encoders and applies to it the given speed.
     * @param motor The motor on which the speed will be set.
     * @param speed The speed to which the motor will be set.
     */
    private void setMotorSpeed(DcMotor motor, double speed) {
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setPower(speed);
    }

    /**
     * Sets the given motor to isFinished to encoder position and
     * applies the given target position and speed.
     * @param motor The motor on which the speed and position will be set.
     * @param position The target position to which the motor will be set.
     * @param speed The speed to which the motor will be set.
     */
    private void setMotorPosition(DcMotor motor, int position, double speed) {
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motor.setTargetPosition(position);
        motor.setPower(speed);
    }

    // =============================================================================================

    public void setVisionEnabled(boolean enabled) {
        if (enabled) {
            this.tracker = new RoverRuckusBirdseyeTracker();
            this.tracker.setShowCameraPreview(true);
            this.tracker.setVuforiaKey(Constants.VUFORIA_KEY);
            this.tracker.setWebcamNames(Constants.POS_CAM, Constants.MINERAL_CAM);
            this.tracker.setCameraForwardOffset(Constants.CAM_X_OFFSET);
            this.tracker.setCameraVerticalOffset(Constants.CAM_Z_OFFSET);
            this.tracker.cameraLeftOffsetMm(Constants.CAM_Y_OFFSET);
            this.tracker.setCameraRotationalOffset(Constants.CAM_R_OFFSET);
            this.tracker.initialize(this.hardwareMap);
            this.tracker.start();
        } else {
            if (this.tracker != null) {
                this.tracker.stop();
                this.tracker = null;
            }
        }
    }

    public void useRearCamera() {
        this.tracker.setActiveWebcam(0);
    }

    public void useSideCamera() {
        this.tracker.setActiveWebcam(1);
    }

    public MineralSample getMineralSample() {
        return this.tracker.trySampleMinerals();
    }

    // =============================================================================================

    public FieldPosition getPosition() {
        if (this.tracker != null) {
            return this.tracker.getCurrentPosition();
        }
        return null;
    }

    private float baseGyroHeading;

    public void resetGyroHeading() {
        baseGyroHeading = getGyroHeading180();
    }

    public float getGyroHeading360() {
        float euler =  getGyroHeading180();
        return MathHelpers.piTo2Pi(euler);
    }

    public float getGyroHeading180() {
        return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle - baseGyroHeading;
    }

    public boolean isReady() {
        return this.isInitialized
            && this.imu.isSystemCalibrated();
    }

    public void stopAllMotors() {
        this.setDriveInput(0, 0, 0);
        this.collector.setPower(0);
        this.extend.setPower(0);
        this.lift.setPower(0);
        this.tracker.stop();
    }

    // =============================================================================================

    private double setX, setY, setZ = 0;
    public void setDriveInput(double x, double y, double z) {
        setX = x;
        setY = y;
        setZ = z;
        this.drive.setInput(setX, setY, setZ);
    }

    public void setDriveInputX(double x) {
        this.drive.setInput(x, setY, setZ);
    }

    public void setDriveInputY(double y) {
        this.drive.setInput(setX, y, setZ);
    }

    public void setDriveInputZ(double z) {
        this.drive.setInput(setX, setY, z);
    }

    public void moveForward(double inches, double speed) {
        this.drive.setForwardTargetPositionRelative(Math.abs(inches), speed);
    }

    public void moveBackward(double inches, double speed) {
        this.drive.setForwardTargetPositionRelative(Math.abs(inches) * -1, speed);
    }

    public boolean isDriveBusy() {
        return this.drive.isBusy();
    }

    // =============================================================================================
    public void setLiftSpeed(double speed) {
        this.lift.setPower(speed);
    }

    public void setLiftPosition(double position, double speed) {
        position = Range.clip(position, 0.0, 1.0);
        int ticks = (int)(position * Constants.MAX_LIFT_TICKS);
        setMotorPosition(this.lift, ticks, speed);
    }

    public int getLiftPosition() {
        return lift.getCurrentPosition() / Constants.MAX_LIFT_TICKS;
    }

    public boolean isLiftBusy() {
        return lift.isBusy();
    }

    // =============================================================================================

    public void setArmSpeed(double speed) {
        setMotorSpeed(this.collector, speed);
    }

    public void setArmPosition(int position, double speed) {
        setMotorPosition(this.collector, position, speed);
    }

    public int getArmPosition() {
        return collector.getCurrentPosition();
    }

    public boolean isArmBusy() {
        return collector.isBusy();
    }

    // =============================================================================================

    public void setExtendSpeed(double speed) {
        setMotorSpeed(this.extend, speed);
    }

    public void setExtedPosition(int position, double speed) {
        setMotorPosition(this.extend, position, speed);
    }

    public int getExtendPosition() {
        return extend.getCurrentPosition();
    }

    // =============================================================================================

    public void collect(boolean left, boolean right) {
        this.collectorLeft.setPosition(left ? 0 : 1);
        this.collectorRight.setPosition(right ? 0 : 1);
    }

    // =============================================================================================

    public class Constants {
        static final String FRONT_LEFT      = "FrontLeft";
        static final String FRONT_RIGHT     = "FrontRight";
        static final String BACK_LEFT       = "BackLeft";
        static final String BACK_RIGHT      = "BackRight";
        static final String LIFT            = "Lift";
        static final String ARM             = "Arm";
        static final String EXTEND          = "Extend";
        static final String COLLECT_LEFT    = "CollectorLeft";
        static final String COLLECT_RIGHT   = "CollectorRight";
        static final String GYRO            = "IMU";
        static final String POS_CAM         = "PositionCamera";
        static final String MINERAL_CAM     = "MineralCamera";
        static final String VUFORIA_KEY     = "AUmjH6X/////AAABmeSd/rs+aU4giLmf5DG5vUaAfHFLv0/vAnAFxt5vM6cbn1/nI2sdkRSEf6HZLA/is/+VQY5/i6u5fbJ4TugEN8HOxRwvUvkrAeIpgnMYEe3jdD+dPxhE88dB58mlPfVwIPJc2KF4RE7weuRBoZ8KlrEKbNNu20ommdG7S/HXP9Kv/xocj82rgj+iPEaitftALZ6QaGBdfSl3nzVMK8/KgQJNlSbGic/Wf3VI8zcYmMyDslQPK45hZKlHW6ezxdGgJ7VJCax+Of8u/LEwfzqDqBsuS4/moNBJ1mF6reBKe1hIE2ffVTSvKa2t95g7ht3Z4M6yQdsI0ZaJ6AGnl1wTlm8Saoal4zTbm/VCsmZI081h";
        static final float CAM_X_OFFSET     = -8.0f;
        static final float CAM_Y_OFFSET     = 0.0f;
        static final float CAM_Z_OFFSET     = 5.5f;
        static final int CAM_R_OFFSET       = 0;

        static final int MAX_LIFT_TICKS     = 3100;
    }
}