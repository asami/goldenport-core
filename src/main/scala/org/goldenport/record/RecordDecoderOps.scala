package org.goldenport.record

import java.nio.file.Path
import org.goldenport.Consequence
import org.goldenport.record.io.RecordSourceLoader

/*
 * @since   Apr.  8, 2026
 * @version Apr.  8, 2026
 * @author  ASAMI, Tomoharu
 */
object RecordDecoderOps:
  extension (record: Record)
    def decodeAs[T](using dec: RecordDecoder[T]): Consequence[T] =
      dec.fromRecord(record)

  extension (path: Path)
    def loadRecord: Consequence[Record] =
      RecordSourceLoader.load(path)

    def loadRecordAs[T](using dec: RecordDecoder[T]): Consequence[T] =
      RecordSourceLoader.decode[T](path)
