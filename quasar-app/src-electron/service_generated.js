import * as flatbuffers from 'flatbuffers';

export const ActionType = {
  ACTION_NONE: 0,
  ACTION_GET_CPU: 1,
  ACTION_GET_MEMORY: 2,
  ACTION_GET_BATTERY: 3,
  ACTION_GET_VOLUME: 4,
  ACTION_GET_MEDIA: 5,
  ACTION_GET_ALL: 6,
  ACTION_PING: 7,
  ACTION_STOP: 8,
  ACTION_GET_PINNED_APPS: 20,
  ACTION_EXTRACT_ICON: 21,
  ACTION_PARSE_LNK: 22,
  ACTION_WATCH_TASKBAR_DIR: 23,
  ACTION_HIDE_SYSTEM_TASKBAR: 24,
  ACTION_SHOW_SYSTEM_TASKBAR: 25
};

export const ResponseStatus = {
  STATUS_OK: 0,
  STATUS_ERROR: 1
};

export const Message = {
  NONE: 0,
  Request: 1,
  Response: 2
};

export class PinnedAppInfo {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsPinnedAppInfo(bb, obj) {
    return (obj || new PinnedAppInfo()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  id(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    console.log('[PinnedAppInfo] id() bb_pos:', this.bb_pos, 'offset:', offset, 'bb:', this.bb ? 'yes' : 'no');
    if (offset) {
      const str = this.bb.__string(this.bb_pos + offset, optionalEncoding);
      console.log('[PinnedAppInfo] id() string:', str);
      return str;
    }
    console.log('[PinnedAppInfo] Checking vtable at bb_pos:', this.bb_pos);
    const vtable = this.bb_pos - this.bb.readInt32(this.bb_pos);
    console.log('[PinnedAppInfo] vtable position:', vtable, 'vtable size:', this.bb.readInt16(vtable));
    return null;
  }

  name(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 6);
    if (offset) {
      const str = this.bb.__string(this.bb_pos + offset, optionalEncoding);
      console.log('[PinnedAppInfo] name() string:', str, 'bytes:', Buffer.from(str, 'utf-8').toString('hex'));
      return str;
    }
    return null;
  }

  path(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  iconData(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  iconWidth() {
    const offset = this.bb.__offset(this.bb_pos, 12);
    return offset ? this.bb.readUint16(this.bb_pos + offset) : 0;
  }

  iconHeight() {
    const offset = this.bb.__offset(this.bb_pos, 14);
    return offset ? this.bb.readUint16(this.bb_pos + offset) : 0;
  }
}

export class PinnedAppsResponse {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsPinnedAppsResponse(bb, obj) {
    return (obj || new PinnedAppsResponse()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  apps(index, obj) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    if (!offset) return null;

    const vectorPos = this.bb.__vector(this.bb_pos + offset);
    const vectorLength = this.bb.__vector_len(this.bb_pos + offset);

    console.log('[PinnedAppsResponse] apps() index:', index);
    console.log('  this.bb_pos:', this.bb_pos, 'offset:', offset);
    console.log('  vectorPos:', vectorPos, 'vectorLength:', vectorLength);

    if (index < 0 || index >= vectorLength) return null;

    const elementPos = this.bb.__indirect(vectorPos + 4 + index * 4);

    console.log('  elementPos:', elementPos, 'Buffer size:', this.bb.bytes_.length);

    if (elementPos >= this.bb.bytes_.length) {
      console.error('[PinnedAppsResponse] elementPos', elementPos, 'exceeds buffer size');
      return null;
    }

    return (obj || new PinnedAppInfo()).__init(elementPos, this.bb);
  }

  appsLength() {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__vector_len(this.bb_pos + offset) : 0;
  }
}

export class Response {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsResponse(bb, obj) {
    return (obj || new Response()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  id() {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.readUint32(this.bb_pos + offset) : 0;
  }

  status() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readUint8(this.bb_pos + offset) : ResponseStatus.STATUS_OK;
  }

  errorMessage(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  cpu(obj) {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? (obj || new CPUInfo()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  memory(obj) {
    const offset = this.bb.__offset(this.bb_pos, 12);
    return offset ? (obj || new MemoryInfo()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  battery(obj) {
    const offset = this.bb.__offset(this.bb_pos, 14);
    return offset ? (obj || new BatteryInfo()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  volume(obj) {
    const offset = this.bb.__offset(this.bb_pos, 16);
    return offset ? (obj || new VolumeInfo()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  media(obj) {
    const offset = this.bb.__offset(this.bb_pos, 18);
    return offset ? (obj || new MediaInfo()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  pinnedApps(obj) {
    const offset = this.bb.__offset(this.bb_pos, 20);
    console.log('[Response] pinnedApps() this.bb_pos:', this.bb_pos, 'offset:', offset);
    return offset ? (obj || new PinnedAppsResponse()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  iconResponse(obj) {
    const offset = this.bb.__offset(this.bb_pos, 22);
    return offset ? (obj || new IconExtractResponse()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }

  lnkResponse(obj) {
    const offset = this.bb.__offset(this.bb_pos, 24);
    return offset ? (obj || new LnkParseResponse()).__init(this.bb.__indirect(this.bb_pos + offset), this.bb) : null;
  }
}

export class MessageWrapper {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsMessageWrapper(bb, obj) {
    return (obj || new MessageWrapper()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  messageTypeType() {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.readUint8(this.bb_pos + offset) : Message.NONE;
  }

  messageType(obj) {
    const offset = this.bb.__offset(this.bb_pos, 6);
    console.log('[MessageWrapper] messageType() this.bb_pos:', this.bb_pos, 'offset:', offset);
    if (offset) {
      const unionOffset = this.bb.readInt32(this.bb_pos + offset);
      const objPos = this.bb_pos + offset + unionOffset;
      console.log('[MessageWrapper] unionOffset:', unionOffset, 'objPos:', objPos);
      obj.bb_pos = objPos;
      obj.bb = this.bb;
      return obj;
    }
    return null;
  }

  static startMessageWrapper(builder) { builder.startObject(2); }
  static addMessageTypeType(builder, type) { builder.addFieldInt8(0, type, Message.NONE); }
  static addMessageType(builder, offset) { builder.addFieldOffset(1, offset, 0); }
  static endMessageWrapper(builder) { return builder.endObject(); }
}

export class Request {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsRequest(bb, obj) {
    return (obj || new Request()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  id() {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.readUint32(this.bb_pos + offset) : 0;
  }

  action() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readUint8(this.bb_pos + offset) : ActionType.ACTION_NONE;
  }

  static startRequest(builder) { builder.startObject(4); }
  static addId(builder, id) { builder.addFieldInt32(0, id, 0); }
  static addAction(builder, action) { builder.addFieldInt8(1, action, ActionType.ACTION_NONE); }
  static addIconRequest(builder, iconRequestOffset) { builder.addFieldOffset(2, iconRequestOffset, 0); }
  static addLnkRequest(builder, lnkRequestOffset) { builder.addFieldOffset(3, lnkRequestOffset, 0); }
  static endRequest(builder) { return builder.endObject(); }
}

class CPUInfo {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsCPUInfo(bb, obj) {
    return (obj || new CPUInfo()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  name(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  usage() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readFloat32(this.bb_pos + offset) : 0;
  }

  coreCount() {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.readUint32(this.bb_pos + offset) : 0;
  }
}

class MemoryInfo {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsMemoryInfo(bb, obj) {
    return (obj || new MemoryInfo()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  total() {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.readUint64(this.bb_pos + offset) : 0;
  }

  available() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readUint64(this.bb_pos + offset) : 0;
  }

  used() {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.readUint64(this.bb_pos + offset) : 0;
  }

  usage() {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.readFloat32(this.bb_pos + offset) : 0;
  }
}

class BatteryInfo {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsBatteryInfo(bb, obj) {
    return (obj || new BatteryInfo()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  acOnline() {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.readBool(this.bb_pos + offset) : false;
  }

  batteryPresent() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readBool(this.bb_pos + offset) : false;
  }

  batteryLifePercent() {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.readUint8(this.bb_pos + offset) : 0;
  }

  batteryLifeTime() {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.readUint32(this.bb_pos + offset) : 0;
  }

  batteryFullLifeTime() {
    const offset = this.bb.__offset(this.bb_pos, 12);
    return offset ? this.bb.readUint32(this.bb_pos + offset) : 0;
  }
}

class VolumeInfo {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsVolumeInfo(bb, obj) {
    return (obj || new VolumeInfo()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  name(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  mountPoint(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  total() {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.readUint64(this.bb_pos + offset) : 0;
  }

  available() {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.readUint64(this.bb_pos + offset) : 0;
  }

  used() {
    const offset = this.bb.__offset(this.bb_pos, 12);
    return offset ? this.bb.readUint64(this.bb_pos + offset) : 0;
  }

  usage() {
    const offset = this.bb.__offset(this.bb_pos, 14);
    return offset ? this.bb.readFloat32(this.bb_pos + offset) : 0;
  }
}

class MediaInfo {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsMediaInfo(bb, obj) {
    return (obj || new MediaInfo()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  title(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  artist(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  albumTitle(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  albumArtist(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  playing() {
    const offset = this.bb.__offset(this.bb_pos, 12);
    return offset ? this.bb.readBool(this.bb_pos + offset) : false;
  }

  position() {
    const offset = this.bb.__offset(this.bb_pos, 14);
    return offset ? this.bb.readFloat64(this.bb_pos + offset) : 0;
  }

  duration() {
    const offset = this.bb.__offset(this.bb_pos, 16);
    return offset ? this.bb.readFloat64(this.bb_pos + offset) : 0;
  }
}

class IconExtractResponse {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsIconExtractResponse(bb, obj) {
    return (obj || new IconExtractResponse()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  iconData(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  width() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readUint16(this.bb_pos + offset) : 0;
  }

  height() {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.readUint16(this.bb_pos + offset) : 0;
  }

  success() {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.readBool(this.bb_pos + offset) : false;
  }
}

class LnkParseResponse {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsLnkParseResponse(bb, obj) {
    return (obj || new LnkParseResponse()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  targetPath(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  arguments(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  workingDirectory(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 8);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  description(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 10);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  iconPath(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 12);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  iconIndex() {
    const offset = this.bb.__offset(this.bb_pos, 14);
    return offset ? this.bb.readInt32(this.bb_pos + offset) : 0;
  }

  success() {
    const offset = this.bb.__offset(this.bb_pos, 16);
    return offset ? this.bb.readBool(this.bb_pos + offset) : false;
  }
}

export class IconExtractRequest {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsIconExtractRequest(bb, obj) {
    return (obj || new IconExtractRequest()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  filePath(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  iconSize() {
    const offset = this.bb.__offset(this.bb_pos, 6);
    return offset ? this.bb.readUint16(this.bb_pos + offset) : 0;
  }

  static startIconExtractRequest(builder) { builder.startObject(2); }
  static addFilePath(builder, filePathOffset) { builder.addFieldOffset(0, filePathOffset, 0); }
  static addIconSize(builder, iconSize) { builder.addFieldInt16(1, iconSize, 0); }
  static endIconExtractRequest(builder) { return builder.endObject(); }
}

export class LnkParseRequest {
  constructor() {
    this.bb = null;
    this.bb_pos = 0;
  }

  __init(i, bb) {
    this.bb_pos = i;
    this.bb = bb;
    return this;
  }

  static getRootAsLnkParseRequest(bb, obj) {
    return (obj || new LnkParseRequest()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
  }

  lnkPath(optionalEncoding) {
    const offset = this.bb.__offset(this.bb_pos, 4);
    return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
  }

  static startLnkParseRequest(builder) { builder.startObject(1); }
  static addLnkPath(builder, lnkPathOffset) { builder.addFieldOffset(0, lnkPathOffset, 0); }
  static endLnkParseRequest(builder) { return builder.endObject(); }
}

export function createRequestMessage(builder, id, actionType) {
  const requestOffset = Request.startRequest(builder);
  Request.addId(builder, id);
  Request.addAction(builder, actionType);
  const requestFinal = Request.endRequest(builder);

  MessageWrapper.startMessageWrapper(builder);
  MessageWrapper.addMessageType(builder, requestFinal);
  MessageWrapper.addMessageTypeType(builder, Message.Request);
  return MessageWrapper.endMessageWrapper(builder);
}

export function parseResponse(buffer) {
  const buf = new flatbuffers.ByteBuffer(buffer);
  const wrapper = MessageWrapper.getRootAsMessageWrapper(buf);

  const messageType = wrapper.messageTypeType();
  if (messageType !== Message.Response) {
    return null;
  }

  return wrapper.messageType(new Response());
}