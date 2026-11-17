//! Android-independent DayLoop domain rules.
//!
//! This crate intentionally uses explicit civil-time value objects instead of
//! platform date/time APIs. Kotlin owns the initial mapping from Room DTOs and
//! remains the persistence authority during the migration.

use std::fmt;

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum ValidationError {
    Blank(&'static str),
    InvalidDate,
    InvalidTime,
    InvalidTimeBlock,
    InvalidReminderPolicy(&'static str),
    InvalidInterval(&'static str),
    InvalidRecurrence(&'static str),
    FocusRequiresTimeBlock,
}

impl fmt::Display for ValidationError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Blank(field) => write!(f, "{field} must not be blank"),
            Self::InvalidDate => write!(f, "invalid civil date"),
            Self::InvalidTime => write!(f, "invalid local time"),
            Self::InvalidTimeBlock => write!(f, "time block start must be before its end"),
            Self::InvalidReminderPolicy(reason) => write!(f, "invalid reminder policy: {reason}"),
            Self::InvalidInterval(reason) => write!(f, "invalid interval: {reason}"),
            Self::InvalidRecurrence(reason) => write!(f, "invalid recurrence: {reason}"),
            Self::FocusRequiresTimeBlock => {
                write!(f, "focus reminder policy requires a time block")
            }
        }
    }
}

impl std::error::Error for ValidationError {}

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct TaskId(String);

impl TaskId {
    pub fn new(value: impl Into<String>) -> Result<Self, ValidationError> {
        let value = value.into();
        if value.trim().is_empty() {
            return Err(ValidationError::Blank("task id"));
        }
        Ok(Self(value))
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }
}

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct ProjectId(String);

impl ProjectId {
    pub fn new(value: impl Into<String>) -> Result<Self, ValidationError> {
        let value = value.into();
        if value.trim().is_empty() {
            return Err(ValidationError::Blank("project id"));
        }
        Ok(Self(value))
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum TaskStatus {
    Active,
    Completed,
    Skipped,
    Someday,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub enum TaskPriority {
    Low,
    Normal,
    High,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ReminderMode {
    Normal,
    Persistent,
    Strict,
    Focus,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct ReminderPolicy {
    pub mode: ReminderMode,
    pub realert_interval_seconds: Option<u64>,
    pub maximum_realerts: u32,
    pub snooze_allowed: bool,
    pub reschedule_allowed: bool,
}

impl ReminderPolicy {
    pub fn normal() -> Self {
        Self {
            mode: ReminderMode::Normal,
            realert_interval_seconds: None,
            maximum_realerts: 0,
            snooze_allowed: true,
            reschedule_allowed: true,
        }
    }

    pub fn validate(&self) -> Result<(), ValidationError> {
        let has_interval = self.realert_interval_seconds.is_some();
        let uses_realerts = self.maximum_realerts > 0;
        if has_interval != uses_realerts {
            return Err(ValidationError::InvalidReminderPolicy(
                "interval and cap must be configured together",
            ));
        }
        if self.realert_interval_seconds == Some(0) {
            return Err(ValidationError::InvalidReminderPolicy(
                "re-alert interval must be positive",
            ));
        }
        let supports_realerts =
            matches!(self.mode, ReminderMode::Persistent | ReminderMode::Strict);
        if supports_realerts != uses_realerts {
            return Err(ValidationError::InvalidReminderPolicy(
                "persistent and strict modes require re-alerts; other modes forbid them",
            ));
        }
        Ok(())
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub struct CivilDate {
    pub year: i32,
    pub month: u8,
    pub day: u8,
}

impl CivilDate {
    pub fn new(year: i32, month: u8, day: u8) -> Result<Self, ValidationError> {
        let max = match month {
            1 | 3 | 5 | 7 | 8 | 10 | 12 => 31,
            4 | 6 | 9 | 11 => 30,
            2 if Self::is_leap_year(year) => 29,
            2 => 28,
            _ => return Err(ValidationError::InvalidDate),
        };
        if day == 0 || day > max {
            return Err(ValidationError::InvalidDate);
        }
        Ok(Self { year, month, day })
    }

    pub const fn is_leap_year(year: i32) -> bool {
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub struct LocalTime {
    pub hour: u8,
    pub minute: u8,
    pub second: u8,
    pub nanosecond: u32,
}

impl LocalTime {
    pub fn new(hour: u8, minute: u8, second: u8, nanosecond: u32) -> Result<Self, ValidationError> {
        if hour > 23 || minute > 59 || second > 59 || nanosecond >= 1_000_000_000 {
            return Err(ValidationError::InvalidTime);
        }
        Ok(Self {
            hour,
            minute,
            second,
            nanosecond,
        })
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub struct LocalDateTime {
    pub date: CivilDate,
    pub time: LocalTime,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Weekday {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum IntervalAnchor {
    ScheduledStart,
    Completion,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct IntervalRule {
    pub every_seconds: u64,
    pub window_start: LocalTime,
    pub window_end: LocalTime,
    pub eligible_days: Vec<Weekday>,
    pub starts_on: CivilDate,
    pub ends_on: Option<CivilDate>,
    pub anchor: IntervalAnchor,
}

impl IntervalRule {
    pub fn validate(&self) -> Result<(), ValidationError> {
        if self.every_seconds == 0 {
            return Err(ValidationError::InvalidInterval(
                "duration must be positive",
            ));
        }
        if self.window_start >= self.window_end {
            return Err(ValidationError::InvalidInterval(
                "overnight or empty windows are unsupported",
            ));
        }
        if self.eligible_days.is_empty() {
            return Err(ValidationError::InvalidInterval(
                "eligible days must not be empty",
            ));
        }
        if self.ends_on.is_some_and(|end| end < self.starts_on) {
            return Err(ValidationError::InvalidInterval(
                "end date precedes start date",
            ));
        }
        Ok(())
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum RecurrenceFrequency {
    Daily,
    Weekly,
    Monthly,
    Yearly,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct RecurrenceRule {
    pub frequency: RecurrenceFrequency,
    pub every: u32,
    pub starts_on: CivilDate,
    pub ends_on: Option<CivilDate>,
    pub days_of_week: Vec<Weekday>,
}

impl RecurrenceRule {
    pub fn validate(&self) -> Result<(), ValidationError> {
        if self.every == 0 {
            return Err(ValidationError::InvalidRecurrence(
                "interval must be positive",
            ));
        }
        if self.ends_on.is_some_and(|end| end < self.starts_on) {
            return Err(ValidationError::InvalidRecurrence(
                "end date precedes start date",
            ));
        }
        let weekly = self.frequency == RecurrenceFrequency::Weekly;
        if weekly != !self.days_of_week.is_empty() {
            return Err(ValidationError::InvalidRecurrence(
                "only weekly recurrence has non-empty weekdays",
            ));
        }
        Ok(())
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum TaskSchedule {
    None,
    AllDay(CivilDate),
    Deadline(LocalDateTime),
    TimeBlock {
        starts_at: LocalDateTime,
        ends_at: LocalDateTime,
    },
    Interval(IntervalRule),
    CalendarRecurrence {
        rule: RecurrenceRule,
        local_time: Option<LocalTime>,
    },
}

impl TaskSchedule {
    pub fn validate(&self) -> Result<(), ValidationError> {
        match self {
            Self::TimeBlock { starts_at, ends_at } if starts_at >= ends_at => {
                Err(ValidationError::InvalidTimeBlock)
            }
            Self::Interval(rule) => rule.validate(),
            Self::CalendarRecurrence { rule, .. } => rule.validate(),
            _ => Ok(()),
        }
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Subtask {
    pub id: String,
    pub title: String,
    pub is_completed: bool,
}

impl Subtask {
    pub fn validate(&self) -> Result<(), ValidationError> {
        if self.id.trim().is_empty() {
            return Err(ValidationError::Blank("subtask id"));
        }
        if self.title.trim().is_empty() {
            return Err(ValidationError::Blank("subtask title"));
        }
        Ok(())
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct TaskProvenance {
    pub origin_type: String,
    pub rule_id: String,
    pub version: i64,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Task {
    pub id: TaskId,
    pub title: String,
    pub status: TaskStatus,
    pub priority: TaskPriority,
    pub schedule: TaskSchedule,
    pub reminder_policy: ReminderPolicy,
    pub project_id: Option<ProjectId>,
    pub subtasks: Vec<Subtask>,
    pub description: Option<String>,
    pub provenance: Option<TaskProvenance>,
    /// UTC epoch milliseconds. Civil schedules stay civil values above.
    pub completed_at_epoch_millis: Option<i64>,
}

impl Task {
    pub fn new(id: TaskId, title: impl Into<String>) -> Result<Self, ValidationError> {
        let task = Self {
            id,
            title: title.into(),
            status: TaskStatus::Active,
            priority: TaskPriority::Normal,
            schedule: TaskSchedule::None,
            reminder_policy: ReminderPolicy::normal(),
            project_id: None,
            subtasks: vec![],
            description: None,
            provenance: None,
            completed_at_epoch_millis: None,
        };
        task.validate()?;
        Ok(task)
    }

    pub fn validate(&self) -> Result<(), ValidationError> {
        if self.title.trim().is_empty() {
            return Err(ValidationError::Blank("task title"));
        }
        self.schedule.validate()?;
        self.reminder_policy.validate()?;
        if self.reminder_policy.mode == ReminderMode::Focus
            && !matches!(self.schedule, TaskSchedule::TimeBlock { .. })
        {
            return Err(ValidationError::FocusRequiresTimeBlock);
        }
        for subtask in &self.subtasks {
            subtask.validate()?;
        }
        Ok(())
    }

    pub fn complete(mut self, at_epoch_millis: i64) -> Self {
        if self.status != TaskStatus::Completed {
            self.status = TaskStatus::Completed;
            self.completed_at_epoch_millis = Some(at_epoch_millis);
        }
        self
    }

    pub fn reopen(mut self) -> Self {
        self.status = TaskStatus::Active;
        self.completed_at_epoch_millis = None;
        self
    }
    pub fn skip(mut self) -> Self {
        self.status = TaskStatus::Skipped;
        self.completed_at_epoch_millis = None;
        self
    }
    pub fn someday(mut self) -> Self {
        self.status = TaskStatus::Someday;
        self.schedule = TaskSchedule::None;
        self.completed_at_epoch_millis = None;
        self.normalize_focus_policy();
        self
    }
    pub fn keep_for_today(mut self, date: CivilDate) -> Self {
        self.status = TaskStatus::Active;
        self.schedule = TaskSchedule::AllDay(date);
        self.completed_at_epoch_millis = None;
        self.normalize_focus_policy();
        self
    }

    fn normalize_focus_policy(&mut self) {
        if self.reminder_policy.mode == ReminderMode::Focus {
            self.reminder_policy = ReminderPolicy::normal();
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn task() -> Task {
        Task::new(TaskId::new("task-1").unwrap(), "Plan day").unwrap()
    }

    #[test]
    fn opaque_ids_reject_blanks() {
        assert!(TaskId::new(" ").is_err());
    }
    #[test]
    fn time_block_requires_order() {
        let d = CivilDate::new(2026, 11, 8).unwrap();
        let noon = LocalDateTime {
            date: d,
            time: LocalTime::new(12, 0, 0, 0).unwrap(),
        };
        assert_eq!(
            TaskSchedule::TimeBlock {
                starts_at: noon,
                ends_at: noon
            }
            .validate(),
            Err(ValidationError::InvalidTimeBlock)
        );
    }
    #[test]
    fn focus_requires_time_block() {
        let mut value = task();
        value.reminder_policy = ReminderPolicy {
            mode: ReminderMode::Focus,
            ..ReminderPolicy::normal()
        };
        assert_eq!(
            value.validate(),
            Err(ValidationError::FocusRequiresTimeBlock)
        );
    }
    #[test]
    fn completion_is_idempotent_and_preserves_first_instant() {
        let done = task().complete(100).complete(200);
        assert_eq!(done.completed_at_epoch_millis, Some(100));
    }
    #[test]
    fn missed_work_transitions_normalize_focus() {
        let d = CivilDate::new(2026, 11, 8).unwrap();
        let time = LocalTime::new(9, 0, 0, 0).unwrap();
        let mut value = task();
        value.schedule = TaskSchedule::TimeBlock {
            starts_at: LocalDateTime { date: d, time },
            ends_at: LocalDateTime {
                date: d,
                time: LocalTime::new(10, 0, 0, 0).unwrap(),
            },
        };
        value.reminder_policy = ReminderPolicy {
            mode: ReminderMode::Focus,
            ..ReminderPolicy::normal()
        };
        assert_eq!(value.someday().reminder_policy, ReminderPolicy::normal());
    }
}
