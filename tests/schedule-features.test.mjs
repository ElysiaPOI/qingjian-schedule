import assert from "node:assert/strict"
import { readFile } from "node:fs/promises"
import test from "node:test"

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), "utf8")

test("weekly schedule uses a touch-following three-panel carousel", async () => {
  const [source, css] = await Promise.all([
    read("app/schedule-app.tsx"),
    read("app/globals.css"),
  ])
  assert.match(source, /startViewSwipe/)
  assert.match(source, /moveViewSwipe/)
  assert.match(source, /finishViewSwipe/)
  assert.match(source, /if \(view === "week"\)/)
  assert.match(source, /const nextWeek = dx < 0 \? week \+ 1 : week - 1/)
  assert.match(source, /Math\.abs\(finalVelocity \|\| start\.velocityX\) >= \.45/)
  assert.match(source, /width \* \.22/)
  assert.match(source, /weekTrack\.current\?\.style\.setProperty\("--week-drag-x"/)
  assert.match(source, /slideWeeks\.map/)
  assert.match(source, /preventClickAfterWeekSwipe/)
  assert.match(source, /if \(dx < 0\) changeView\("week"\)/)
  assert.match(css, /touch-action:\s*pan-y/)
  assert.match(css, /\.week-track/)
  assert.match(css, /translate3d\(calc\(-100% \+ var\(--week-drag-x\)\)/)
  assert.match(css, /\.28s cubic-bezier\(\.22, 1, \.36, 1\)/)
})

test("weekly timetable can return to the current week or today", async () => {
  const [source, css] = await Promise.all([
    read("app/schedule-app.tsx"),
    read("app/globals.css"),
  ])
  assert.match(source, /function returnToCurrentWeek/)
  assert.match(source, /回到本周/)
  assert.match(source, /回到今天/)
  assert.match(source, /view === "week" && awayFromCurrentWeek/)
  assert.match(css, /\.today-button \{[^}]*border:\s*1px solid/s)
})

test("official holidays and adjusted workdays are shown in date headers", async () => {
  const [source, holidays, css] = await Promise.all([
    read("app/schedule-app.tsx"),
    read("lib/china-holidays.ts"),
    read("app/globals.css"),
  ])
  assert.match(source, /holidayForDate/)
  assert.match(source, /holiday-marker/)
  assert.match(source, /holiday-summary/)
  assert.match(source, /table-holiday/)
  assert.match(holidays, /"2026-09-25": \{ name: "中秋节", kind: "holiday" \}/)
  assert.match(holidays, /"2026-09-20": \{ name: "国庆节调休", kind: "workday" \}/)
  assert.match(holidays, /"2026-10-10": \{ name: "国庆节调休", kind: "workday" \}/)
  assert.match(css, /\.holiday-marker\.workday/)
})

test("current day is outlined in the weekly timetable", async () => {
  const [source, css] = await Promise.all([
    read("app/schedule-app.tsx"),
    read("app/globals.css"),
  ])
  assert.match(source, /week === todayWeek/)
  assert.match(source, /today-column-highlight/)
  assert.match(source, /gridColumn: `\$\{todayDay \+ 1\} \/ \$\{todayDay \+ 2\}`/)
  assert.match(css, /\.today-column-highlight/)
  assert.match(css, /border:\s*2px dashed/)
})

test("web imports omit unknown teachers from the main timetable", async () => {
  const source = await read("app/schedule-app.tsx")
  assert.match(source, /course\.teachers\.length > 0 && <span><UserRound/)
  assert.match(source, /任课教师<\/small>\{course\.teachers\.join\("、"\) \|\| "未获取"/)
  assert.match(source, /parsed\.source === "web" \? \[\]/)
  assert.match(source, /schedule\.courses\.map\(\(course\) => \(\{ \.\.\.course, teachers: \[\] \}\)\)/)
  assert.doesNotMatch(source, /教师未获取/)
})
