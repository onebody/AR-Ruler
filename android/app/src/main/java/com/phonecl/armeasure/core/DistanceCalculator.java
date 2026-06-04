package com.phonecl.armeasure.core;

import java.util.Locale;

public class DistanceCalculator {

    public static final String UNIT_METER = "m";
    public static final String UNIT_FEET = "ft";
    public static final String UNIT_INCH = "in";
    public static final String UNIT_CENTIMETER = "cm";
    
    private boolean isMetric = true;
    private boolean useCentimeters = false;
    
    // ARCore测量精度参数
    private static final float MIN_DISTANCE_THRESHOLD = 0.01f; // 最小测量距离 1cm
    private static final float MAX_DISTANCE_THRESHOLD = 100.0f; // 最大测量距离 100m
    private static final float HEIGHT_THRESHOLD = 0.005f; // 高度测量精度阈值 5mm

    public void setMetric(boolean metric) {
        this.isMetric = metric;
    }

    public void setUseCentimeters(boolean useCentimeters) {
        this.useCentimeters = useCentimeters;
    }

    public boolean isMetric() {
        return isMetric;
    }

    /**
     * 计算3D空间中两点之间的欧几里得距离
     * @param startPose 起点坐标 [x, y, z]
     * @param endPose 终点坐标 [x, y, z]
     * @return 距离（米）
     */
    public double calculateDistance(float[] startPose, float[] endPose) {
        if (startPose == null || endPose == null || startPose.length < 3 || endPose.length < 3) {
            return 0.0;
        }
        
        float dx = endPose[0] - startPose[0];
        float dy = endPose[1] - startPose[1];
        float dz = endPose[2] - startPose[2];
        
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // 应用精度阈值过滤
        if (distance < MIN_DISTANCE_THRESHOLD) {
            return 0.0;
        }
        
        return distance;
    }

    /**
     * 计算两点之间的垂直高度差
     * @param startPose 起点坐标 [x, y, z]
     * @param endPose 终点坐标 [x, y, z]
     * @return 高度差（米）
     */
    public double calculateHeight(float[] startPose, float[] endPose) {
        if (startPose == null || endPose == null || startPose.length < 3 || endPose.length < 3) {
            return 0.0;
        }
        
        double height = Math.abs(endPose[1] - startPose[1]);
        
        // 应用精度阈值过滤
        if (height < HEIGHT_THRESHOLD) {
            return 0.0;
        }
        
        return height;
    }

    /**
     * 计算水平距离（忽略高度差）
     * @param startPose 起点坐标 [x, y, z]
     * @param endPose 终点坐标 [x, y, z]
     * @return 水平距离（米）
     */
    public double calculateHorizontalDistance(float[] startPose, float[] endPose) {
        if (startPose == null || endPose == null || startPose.length < 3 || endPose.length < 3) {
            return 0.0;
        }
        
        float dx = endPose[0] - startPose[0];
        float dz = endPose[2] - startPose[2];
        
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * 计算两点之间的角度（相对于水平面）
     * @param startPose 起点坐标 [x, y, z]
     * @param endPose 终点坐标 [x, y, z]
     * @return 角度（度）
     */
    public double calculateAngle(float[] startPose, float[] endPose) {
        double horizontalDistance = calculateHorizontalDistance(startPose, endPose);
        double verticalDistance = calculateHeight(startPose, endPose);
        
        if (horizontalDistance < MIN_DISTANCE_THRESHOLD) {
            return 90.0; // 垂直方向
        }
        
        return Math.toDegrees(Math.atan2(verticalDistance, horizontalDistance));
    }

    /**
     * 将米转换为当前单位
     * @param meters 米
     * @return 转换后的值
     */
    public double convertToUnit(double meters) {
        if (!isMetric) {
            // 英制单位
            if (useCentimeters) {
                return meters * 39.3701; // 英寸
            } else {
                return meters * 3.28084; // 英尺
            }
        } else {
            // 公制单位
            if (useCentimeters) {
                return meters * 100.0; // 厘米
            } else {
                return meters; // 米
            }
        }
    }

    /**
     * 获取当前单位字符串
     * @return 单位字符串
     */
    public String getUnit() {
        if (!isMetric) {
            return useCentimeters ? UNIT_INCH : UNIT_FEET;
        } else {
            return useCentimeters ? UNIT_CENTIMETER : UNIT_METER;
        }
    }

    /**
     * 格式化测量结果
     * @param value 测量值
     * @param isHeight 是否为高度测量
     * @return 格式化后的字符串
     */
    public String formatResult(double value, boolean isHeight) {
        String unit = getUnit();
        String label = isHeight ? "高度" : "距离";
        
        if (value < MIN_DISTANCE_THRESHOLD) {
            return String.format(Locale.getDefault(), "%s: 过近", label);
        }
        
        if (value > MAX_DISTANCE_THRESHOLD) {
            return String.format(Locale.getDefault(), "%s: 过远", label);
        }
        
        return String.format(Locale.getDefault(), "%s: %.2f %s", label, value, unit);
    }

    /**
     * 格式化原始数值
     * @param value 数值
     * @return 格式化后的字符串
     */
    public String formatRaw(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    /**
     * 验证测量结果的有效性
     * @param value 测量值
     * @return 是否有效
     */
    public boolean isValidMeasurement(double value) {
        return value >= MIN_DISTANCE_THRESHOLD && value <= MAX_DISTANCE_THRESHOLD;
    }

    /**
     * 获取测量精度等级
     * @param distance 测量距离
     * @return 精度等级字符串
     */
    public String getAccuracyLevel(double distance) {
        if (distance < 1.0) {
            return "高精度 (±1cm)";
        } else if (distance < 5.0) {
            return "中精度 (±3cm)";
        } else if (distance < 20.0) {
            return "低精度 (±10cm)";
        } else {
            return "估算精度 (±20cm)";
        }
    }

    /**
     * 计算多个点的平均距离
     * @param points 点坐标数组 [x1,y1,z1, x2,y2,z2, ...]
     * @return 平均距离
     */
    public double calculateAverageDistance(float[] points) {
        if (points == null || points.length < 6) {
            return 0.0;
        }
        
        double totalDistance = 0.0;
        int count = 0;
        
        for (int i = 0; i < points.length - 3; i += 3) {
            float[] point1 = {points[i], points[i+1], points[i+2]};
            float[] point2 = {points[i+3], points[i+4], points[i+5]};
            totalDistance += calculateDistance(point1, point2);
            count++;
        }
        
        return count > 0 ? totalDistance / count : 0.0;
    }

    /**
     * 获取测量建议
     * @param distance 测量距离
     * @return 建议字符串
     */
    public String getMeasurementAdvice(double distance) {
        if (distance < 0.5) {
            return "建议：距离过近，测量精度可能降低";
        } else if (distance > 50.0) {
            return "建议：距离过远，建议分段测量";
        } else if (distance > 10.0) {
            return "建议：长距离测量，建议使用辅助点";
        } else {
            return "建议：测量距离适中，精度良好";
        }
    }
}