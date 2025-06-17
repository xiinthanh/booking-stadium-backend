package com.ouroboros.pestadiumbookingbe.util;
// IcsFileGenerator.java
import com.ouroboros.pestadiumbookingbe.dto.BookingSummary;
import com.ouroboros.pestadiumbookingbe.model.Booking;
import com.ouroboros.pestadiumbookingbe.model.TimeSlot;
import com.ouroboros.pestadiumbookingbe.service.TimeSlotService;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.UUID;

@Component
public class IcsFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(IcsFileGenerator.class);
    private final TimeSlotService timeSlotService;

    @Autowired
    public IcsFileGenerator(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    public ByteArrayOutputStream generateIcsStream(BookingSummary bookingSummary) throws Exception {
        if (bookingSummary == null) {
            logger.info("Booking is null when generating ics file");
            return new ByteArrayOutputStream();
        }
        if (bookingSummary.getBookingDate() == null || bookingSummary.getStartTime() == null || bookingSummary.getEndTime() == null) {
            logger.info("Booking date or time is null when generating ics file");
            return new ByteArrayOutputStream();
        }
        if (bookingSummary.getSportHallName() == null || bookingSummary.getPurpose() == null) {
            logger.info("Sport hall name or purpose is null when generating ics file");
            return new ByteArrayOutputStream();
        }

        LocalTime startTime = bookingSummary.getStartTime();
        LocalTime endTime = bookingSummary.getEndTime();

        ZonedDateTime start = ZonedDateTime.of(bookingSummary.getBookingDate(), startTime, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.of(bookingSummary.getBookingDate(), endTime, ZoneId.systemDefault());

        DateTime startDateTime = new DateTime(start.toInstant().toEpochMilli());
        DateTime endDateTime = new DateTime(end.toInstant().toEpochMilli());

        VEvent meeting = new VEvent();
        meeting.getProperties().add(new DtStart(startDateTime));
        meeting.getProperties().add(new DtEnd(endDateTime));
        meeting.getProperties().add(new Summary("Booking for " + bookingSummary.getSportHallName()));

        meeting.getProperties().add(new Description("Purpose: " + bookingSummary.getPurpose()));
        meeting.getProperties().add(new Uid(UUID.randomUUID().toString()));
        meeting.getProperties().add(new Location("Sport Hall: " + bookingSummary.getSportHallName()));

        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//PE Stadium Booking//iCal4j 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        calendar.getComponents().add(meeting);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(calendar, out);
        return out;
    }
}
