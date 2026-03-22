import { labs as seedLabs } from "./labs";

function nowIso() {
  return new Date().toISOString();
}

export function createSeedData() {
  const now = nowIso();

  const users = [
    {
      id: 1,
      email: "student@example.com",
      password: "123456",
      role: "student",
      realName: "学生 张三",
      status: "active",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 2,
      email: "teacher@example.com",
      password: "123456",
      role: "teacher",
      realName: "教师 李老师",
      status: "active",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 3,
      email: "admin@example.com",
      password: "123456",
      role: "admin",
      realName: "管理员",
      status: "active",
      createdAt: now,
      updatedAt: now
    }
  ];

  // 设备（示例）
  const devices = [
    {
      id: 1,
      name: "高性能计算机 #01",
      category: "通用计算机设备",
      location: "实验楼A-201",
      labId: 1,
      assetCode: "A-2024-001",
      status: "available",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 2,
      name: "显微镜 BIO-02",
      category: "精密仪器",
      location: "实验楼B-301",
      labId: 3,
      assetCode: "B-2023-112",
      status: "maintenance",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 3,
      name: "3D打印机 #02",
      category: "教学实验装置",
      location: "实验楼C-101",
      labId: null,
      assetCode: "C-2022-077",
      status: "available",
      createdAt: now,
      updatedAt: now
    }
  ];

  // 实验室（沿用现有 mock/labs.js，并补齐字段）
  const labs = seedLabs.map((lab) => ({
    id: lab.id,
    name: lab.name,
    type: lab.type,
    building: lab.building,
    college: lab.college,
    capacity: lab.capacity,
    opening: lab.opening,
    description: lab.description,
    manager: lab.manager,
    image: lab.image,
    status: lab.status === "AVAILABLE" ? "available" : lab.status === "BOOKED" ? "booked" : "maintenance",
    createdAt: now,
    updatedAt: now
  }));

  // 预约（示例，供教师审批/报表联动）
  const bookings = [
    {
      id: 1,
      createdByUserId: 1,
      resourceType: "lab",
      resourceId: 1,
      resourceName: "计算机实验室A301",
      startAt: `${now.substring(0, 10)}T14:00:00.000Z`,
      endAt: `${now.substring(0, 10)}T16:00:00.000Z`,
      purpose: "数据结构课程实验",
      participants: 1,
      isEmergency: false,
      status: "pending",
      reviewerUserId: 2,
      reviewNote: "",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 2,
      createdByUserId: 1,
      resourceType: "device",
      resourceId: 1,
      resourceName: "高性能计算机 #01",
      startAt: `${now.substring(0, 10)}T09:00:00.000Z`,
      endAt: `${now.substring(0, 10)}T11:00:00.000Z`,
      purpose: "机器学习训练",
      participants: 1,
      isEmergency: true,
      status: "pending",
      reviewerUserId: 2,
      reviewNote: "",
      createdAt: now,
      updatedAt: now
    }
  ];

  // 报修（示例）
  const repairs = [
    {
      id: 1,
      createdByUserId: 1,
      resourceType: "device",
      resourceId: 2,
      resourceName: "显微镜 BIO-02",
      description: "对焦环卡顿，成像有明显噪点",
      status: "submitted",
      handlerUserId: 2,
      createdAt: now,
      updatedAt: now
    }
  ];

  // 申请单（示例：实验室/设备/报废）
  const applications = [
    {
      id: 1,
      type: "lab_apply",
      createdByUserId: 2,
      title: "新增“人工智能实验室”",
      detail: "拟新增实验室用于AI课程实验与竞赛训练",
      status: "submitted",
      reviewedByUserId: null,
      reviewNote: "",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 2,
      type: "device_apply",
      createdByUserId: 2,
      title: "GPU服务器采购申请",
      detail: "申请采购 GPU 服务器 2 台，用于深度学习课程与科研",
      status: "submitted",
      reviewedByUserId: null,
      reviewNote: "",
      createdAt: now,
      updatedAt: now
    },
    {
      id: 3,
      type: "scrap_apply",
      createdByUserId: 2,
      title: "老旧示波器报废",
      detail: "设备老化，维护成本高，申请报废 3 台",
      status: "submitted",
      reviewedByUserId: null,
      reviewNote: "",
      createdAt: now,
      updatedAt: now
    }
  ];

  const notices = [
    { id: 1, title: "计算机实验室A301维护通知", time: "今天 10:20", createdAt: now, updatedAt: now },
    { id: 2, title: "生物实验室本周消杀安排", time: "昨天 16:40", createdAt: now, updatedAt: now }
  ];

  return {
    version: 1,
    users,
    labs,
    devices,
    bookings,
    repairs,
    applications,
    notices
  };
}

