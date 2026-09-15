export type CourseOverride = {
  id: string
  week: number
  cancelled?: boolean
  day?: number
  startSection?: number
  endSection?: number
  room?: string
  teachers?: string[]
}

export type Course = {
  id: string
  code?: string
  name: string
  teachers: string[]
  room: string
  day: number
  startSection: number
  endSection: number
  weeks: number[]
  color: number
  custom?: boolean
  note?: string
  overrides?: CourseOverride[]
  adjusted?: boolean
  cancelled?: boolean
}

export type Schedule = {
  term: string
  startsOn: string
  importedAt: string
  source: "sample" | "pdf" | "file" | "web"
  courses: Course[]
}

const weeks = (end: number) => Array.from({ length: end }, (_, index) => index + 1)

export const initialSchedule: Schedule = {
  term: "2026-2027学年第一学期",
  startsOn: "2026-08-31",
  importedAt: "2026-09-02T00:00:00.000Z",
  source: "sample",
  courses: [
    { id: "sample-math", name: "高等数学", teachers: ["示例教师"], room: "示例教学楼 A101", day: 1, startSection: 1, endSection: 2, weeks: weeks(16), color: 0 },
    { id: "sample-english", name: "大学英语", teachers: ["示例教师"], room: "示例教学楼 B205", day: 2, startSection: 3, endSection: 4, weeks: weeks(16), color: 1 },
    { id: "sample-programming", name: "程序设计", teachers: ["示例教师"], room: "示例教学楼 C301", day: 3, startSection: 5, endSection: 6, weeks: weeks(12), color: 2 },
    { id: "sample-physics", name: "大学物理", teachers: ["示例教师"], room: "示例实验楼 201", day: 4, startSection: 7, endSection: 8, weeks: weeks(16), color: 3 },
    { id: "sample-sports", name: "体育", teachers: ["示例教师"], room: "示例田径场", day: 5, startSection: 1, endSection: 2, weeks: [2, 4, 6, 8, 10, 12, 14, 16], color: 4 },
  ],
}

export const dayNames = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]

export const timeSlots = [
  { start: 1, end: 2, phase: "上午", time: "08:00–09:35" },
  { start: 3, end: 4, phase: "上午", time: "10:05–11:40" },
  { start: 5, end: 6, phase: "下午", time: "13:30–15:05" },
  { start: 7, end: 8, phase: "下午", time: "15:35–17:10" },
  { start: 9, end: 10, phase: "晚上", time: "18:00–19:35" },
  { start: 11, end: 12, phase: "晚上", time: "20:05–21:40" },
]

export function currentWeek(startsOn: string, now = new Date()) {
  const [year, month, day] = startsOn.split("-").map(Number)
  const start = new Date(year, month - 1, day)
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.max(1, Math.floor((today.getTime() - start.getTime()) / 604800000) + 1)
}

export function dateForWeekday(startsOn: string, week: number, weekday: number) {
  const [year, month, day] = startsOn.split("-").map(Number)
  const date = new Date(year, month - 1, day)
  date.setDate(date.getDate() + (week - 1) * 7 + weekday - 1)
  return date
}

export function weekRange(startsOn: string, week: number) {
  const start = dateForWeekday(startsOn, week, 1)
  const end = dateForWeekday(startsOn, week, 7)
  const startText = `${start.getMonth() + 1}月${start.getDate()}日`
  const endText = `${end.getMonth() + 1}月${end.getDate()}日`
  return `${startText}—${endText}`
}

export function compactWeeks(values: number[]) {
  if (!values.length) return "周次待定"
  const sorted = [...new Set(values)].sort((a, b) => a - b)
  const alternating = sorted.length > 2 && sorted.every((value, i) => i === 0 || value - sorted[i - 1] === 2)
  if (alternating) return `${sorted[0]}–${sorted.at(-1)}周（${sorted[0] % 2 ? "单" : "双"}周）`
  const ranges: string[] = []
  let start = sorted[0]
  let end = sorted[0]
  for (const value of sorted.slice(1)) {
    if (value === end + 1) end = value
    else {
      ranges.push(start === end ? `${start}` : `${start}–${end}`)
      start = end = value
    }
  }
  ranges.push(start === end ? `${start}` : `${start}–${end}`)
  return `${ranges.join("、")}周`
}
