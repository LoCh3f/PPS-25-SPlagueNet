package it.unibo.splague.view.form

import it.unibo.splague.model.Probability
import it.unibo.splague.model.connection.Connection.Channel
import it.unibo.splague.model.connection.Connection.ChannelType

final case class ChannelForm(
    channelType: ChannelType,
    bandwidth: String,
    latency: String,
    jitter: String,
    packetLoss: String
)

object ChannelForm:

  def fromChannel(channel: Channel): ChannelForm =
    ChannelForm(
      channelType = channel.channelType,
      bandwidth = channel.bandwidth.toString,
      latency = channel.latency.toString,
      jitter = channel.jitter.toString,
      packetLoss = channel.packetLoss.value.toString
    )

  def toDomain(form: ChannelForm): Either[String, Channel] =
    for
      bw <- parseDouble(form.bandwidth, "bandwidth")
      lat <- parseDouble(form.latency, "latency")
      jit <- parseDouble(form.jitter, "jitter")
      loss <- parseDouble(form.packetLoss, "packetLoss")
    yield Channel(
      channelType = form.channelType,
      bandwidth = bw,
      latency = lat,
      jitter = jit,
      packetLoss = Probability.clamped(loss)
    )

  private def parseDouble(s: String, field: String): Either[String, Double] =
    s.trim.toDoubleOption.toRight(s"$field must be a number, got '$s'")
