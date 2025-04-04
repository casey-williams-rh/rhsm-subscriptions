/*
 * Copyright Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.subscriptions.db;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.candlepin.subscriptions.FixedClockConfiguration;
import org.candlepin.subscriptions.db.model.EventRecord;
import org.candlepin.subscriptions.json.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class EventRecordRepositoryTest {
  private static final Clock CLOCK = new FixedClockConfiguration().fixedClock().getClock();

  @Autowired private EventRecordRepository repository;

  @Test
  void saveAndUpdate() {
    Event event = new Event();
    event.setAccountNumber("account123");
    event.setOrgId("org123");
    event.setTimestamp(OffsetDateTime.now(CLOCK));
    event.setInstanceId("instanceId");
    event.setServiceType("RHEL System");
    UUID eventId = UUID.randomUUID();
    event.setEventId(eventId);
    event.setEventSource("eventSource");
    event.setDisplayName(Optional.empty());

    EventRecord record = new EventRecord(event);
    repository.saveAndFlush(record);

    EventRecord found = repository.getOne(eventId);
    assertNull(found.getEvent().getInventoryId());
    assertNotNull(found.getEvent().getDisplayName());
    assertFalse(found.getEvent().getDisplayName().isPresent());
    assertEquals(record, found);
  }

  @Test
  void testFindBeginInclusive() {
    Event oldEvent =
        event(
            "account123",
            "org123",
            "SOURCE",
            "TYPE",
            "INSTANCE",
            OffsetDateTime.now(CLOCK).minusSeconds(1));
    Event currentEvent =
        event("account123", "org123", "SOURCE", "TYPE", "INSTANCE", OffsetDateTime.now(CLOCK));

    repository.saveAll(Arrays.asList(new EventRecord(oldEvent), new EventRecord(currentEvent)));
    repository.flush();

    List<EventRecord> found =
        repository
            .findByAccountNumberAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestamp(
                "account123", OffsetDateTime.now(CLOCK), OffsetDateTime.now(CLOCK).plusYears(1))
            .collect(Collectors.toList());

    assertEquals(1, found.size());
    assertEquals(currentEvent.getEventId(), found.get(0).getId());
  }

  @Test
  void testFindEndExclusive() {
    EventRecord futureEvent =
        new EventRecord(
            event("account123", "org123", "SOURCE", "TYPE", "INSTANCE", OffsetDateTime.now(CLOCK)));

    EventRecord currentEvent =
        new EventRecord(
            event(
                "account123",
                "org123",
                "SOURCE",
                "TYPE",
                "INSTANCE",
                OffsetDateTime.now(CLOCK).minusSeconds(1)));

    repository.saveAll(Arrays.asList(futureEvent, currentEvent));
    repository.flush();

    List<EventRecord> found =
        repository
            .findByAccountNumberAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestamp(
                "account123", OffsetDateTime.now(CLOCK).minusYears(1), OffsetDateTime.now(CLOCK))
            .collect(Collectors.toList());

    assertEquals(1, found.size());
    assertEquals(currentEvent.getId(), found.get(0).getId());
  }

  @SuppressWarnings({"linelength", "indentation"})
  @Test
  void findBySourceAndType() {
    EventRecord e1 =
        new EventRecord(
            event("account123", "org123", "SOURCE", "TYPE", "INSTANCE", OffsetDateTime.now(CLOCK)));
    EventRecord e2 =
        new EventRecord(
            event(
                "account123",
                "org123",
                "ANOTHER_SOURCE",
                "ANOTHER_TYPE",
                "INSTANCE",
                OffsetDateTime.now(CLOCK).minusSeconds(1)));
    EventRecord e3 =
        new EventRecord(
            event(
                "account123",
                "org123",
                "SOURCE",
                "ANOTHER_TYPE",
                "INSTANCE",
                OffsetDateTime.now(CLOCK)));
    EventRecord e4 =
        new EventRecord(
            event(
                "account123",
                "org123",
                "ANOTHER_SOURCE",
                "TYPE",
                "INSTANCE",
                OffsetDateTime.now(CLOCK)));

    repository.saveAll(List.of(e1, e2, e3, e4));
    repository.flush();

    List<EventRecord> found =
        repository
            .findByAccountNumberAndEventSourceAndEventTypeAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestamp(
                e1.getAccountNumber(),
                e1.getEventSource(),
                e1.getEventType(),
                OffsetDateTime.now(CLOCK).minusYears(1),
                OffsetDateTime.now(CLOCK).plusYears(1))
            .collect(Collectors.toList());

    assertEquals(1, found.size());
    assertEquals(e1, found.get(0));
  }

  @Test
  void testUniqueConstraints() {
    EventRecord e1 =
        new EventRecord(
            event(
                "account123",
                "org123",
                "ANOTHER_SOURCE",
                "TYPE",
                "INSTANCE",
                OffsetDateTime.now(CLOCK)));

    repository.saveAndFlush(e1);

    EventRecord e2 =
        new EventRecord(
            event(
                "account123",
                "org123",
                "ANOTHER_SOURCE",
                "TYPE",
                "INSTANCE",
                OffsetDateTime.now(CLOCK)));

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(e2));
  }

  @Test
  void testDeleteByTimestamp() {
    var now = OffsetDateTime.now();

    EventRecord event =
        EventRecord.builder()
            .id(UUID.randomUUID())
            .accountNumber("bananas1")
            .timestamp(now.minusDays(91L))
            .build();
    EventRecord event2 =
        EventRecord.builder()
            .id(UUID.randomUUID())
            .accountNumber("bananas1")
            .timestamp(now.minusDays(1L))
            .build();

    repository.saveAll(List.of(event, event2));

    repository.deleteEventRecordsByTimestampBefore(now.minusDays(30L));

    var results = repository.findAll();

    assertEquals(1, results.size());
  }

  private Event event(
      String account,
      String orgId,
      String source,
      String type,
      String instanceId,
      OffsetDateTime time) {
    UUID eventId = UUID.randomUUID();
    Event event = new Event();
    event.setEventId(eventId);
    event.setAccountNumber(account);
    event.setOrgId(orgId);
    event.setTimestamp(time);
    event.setInstanceId(instanceId);
    event.setEventSource(source);
    event.setServiceType("SERVICE_TYPE");
    event.setEventType(type);
    event.setDisplayName(Optional.empty());
    return event;
  }
}
