export const labs = [
  {
    id: 1,
    name: "计算机实验室A301",
    type: "COMPUTER",
    building: "A栋",
    college: "信息工程学院",
    capacity: 60,
    status: "AVAILABLE",
    description: "用于程序设计、人工智能课程与上机实践。",
    opening: "08:00-21:00",
    manager: "刘老师",
    image: "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=400&h=250&fit=crop"
  },
  {
    id: 2,
    name: "计算机实验室A402",
    type: "COMPUTER",
    building: "A栋",
    college: "人工智能学院",
    capacity: 48,
    status: "BOOKED",
    description: "用于模型训练课程与GPU资源实验。",
    opening: "09:00-20:30",
    manager: "张老师",
    image: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=250&fit=crop"
  },
  {
    id: 3,
    name: "生物实验室B201",
    type: "BIOLOGY",
    building: "B栋",
    college: "生物工程学院",
    capacity: 30,
    status: "AVAILABLE",
    description: "用于细胞观察、生物样本基础实验。",
    opening: "08:30-20:00",
    manager: "周老师",
    image: "https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400&h=250&fit=crop"
  },
  {
    id: 4,
    name: "生物实验室B305",
    type: "BIOLOGY",
    building: "B栋",
    college: "生命科学学院",
    capacity: 24,
    status: "MAINTENANCE",
    description: "用于PCR相关实验，当前维修中。",
    opening: "暂停开放",
    manager: "王老师",
    image: "https://images.unsplash.com/photo-1581093458791-9d42e1d6b4f4?w=400&h=250&fit=crop"
  },
  {
    id: 5,
    name: "计算机实验室A205",
    type: "COMPUTER",
    building: "A栋",
    college: "信息工程学院",
    capacity: 50,
    status: "AVAILABLE",
    description: "用于网络安全和操作系统实验。",
    opening: "08:00-18:00",
    manager: "陈老师",
    image: "https://images.unsplash.com/photo-1488590528505-98d2b5aba04b?w=400&h=250&fit=crop"
  },
  {
    id: 6,
    name: "生物实验室B108",
    type: "BIOLOGY",
    building: "B栋",
    college: "生物工程学院",
    capacity: 28,
    status: "BOOKED",
    description: "用于生化分析实验与课程实训。",
    opening: "09:00-19:00",
    manager: "李老师",
    image: "https://images.unsplash.com/photo-1576086213369-97a306d36557?w=400&h=250&fit=crop"
  }
];

export function statusClass(status) {
  if (status === "AVAILABLE") return "bg-emerald-100 text-emerald-700 ring-1 ring-emerald-600/20";
  if (status === "BOOKED") return "bg-amber-100 text-amber-700 ring-1 ring-amber-600/20";
  if (status === "MAINTENANCE") return "bg-rose-100 text-rose-700 ring-1 ring-rose-600/20";
  return "bg-slate-100 text-slate-600 ring-1 ring-slate-600/20";
}

export function statusText(status) {
  if (status === "AVAILABLE") return "可用";
  if (status === "BOOKED") return "预约中";
  if (status === "MAINTENANCE") return "维修中";
  return status;
}
