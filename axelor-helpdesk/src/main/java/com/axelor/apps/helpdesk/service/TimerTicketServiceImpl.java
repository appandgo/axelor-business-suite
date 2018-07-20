package com.axelor.apps.helpdesk.service;

import com.axelor.apps.base.db.Timer;
import com.axelor.apps.base.db.TimerHistory;
import com.axelor.apps.base.db.repo.TimerHistoryRepository;
import com.axelor.apps.base.db.repo.TimerRepository;
import com.axelor.apps.base.service.timer.AbstractTimerService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.helpdesk.db.repo.TicketRepository;
import com.axelor.auth.db.User;
import com.axelor.db.Model;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;

public class TimerTicketServiceImpl extends AbstractTimerService implements TimerTicketService {

  protected TicketRepository repository;

  @Inject
  public TimerTicketServiceImpl(
      TimerRepository timerRepository,
      TimerHistoryRepository timerHistoryRepository,
      UserService userService,
      TicketRepository repository) {
    super(timerRepository, timerHistoryRepository, userService);
    this.repository = repository;
  }

  @Override
  public Timer find(Model model) throws AxelorException {
    User user = userService.getUser();
    Ticket ticket = (Ticket) model;

    try {
      return ticket
          .getTimerList()
          .stream()
          .filter(t -> t.getAssignedToUser() == user)
          .findFirst()
          .orElse(null);
    } catch (NullPointerException e) {
      throw new AxelorException(
          TraceBackRepository.TYPE_TECHNICAL,
          "Veuillez recharger la page pour effectuer cette action");
    }
  }

  @Override
  @Transactional
  public TimerHistory start(Model model, Timer timer, LocalDateTime dateTime)
      throws AxelorException {
    Ticket ticket = (Ticket) model;

    boolean isNewTimer = timer == null;
    timer = tryStartOrCreate(timer);
    if (isNewTimer) {
      ticket.addTimerListItem(timer);
    }

    TimerHistory history = new TimerHistory();
    history.setStartDateT(dateTime);
    history.setTimer(timer);
    timer.addTimerHistoryListItem(history);

    return timerHistoryRepository.save(history);
  }

  @Override
  public Timer find(Ticket ticket) throws AxelorException {
    return find((Model) ticket);
  }

  @Override
  public TimerHistory start(Ticket task, LocalDateTime dateTime) throws AxelorException {
    Timer timer = find(task);
    return start(task, timer, dateTime);
  }

  @Override
  public TimerHistory stop(Ticket task, LocalDateTime dateTime) throws AxelorException {
    Timer timer = find(task);
    if (timer != null) {
      return stop(task, timer, dateTime);
    }
    return null;
  }

  @Override
  public void cancel(Ticket task) throws AxelorException {
    Timer timer = find(task);
    cancel(timer);
  }

  @Override
  public Duration compute(Ticket task) {
    Duration total = Duration.ZERO;
    for (Timer timer : task.getTimerList()) {
      total = total.plus(compute(timer));
    }
    return total;
  }
}
