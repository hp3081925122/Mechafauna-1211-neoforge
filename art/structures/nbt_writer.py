# 最小 NBT 写入器，用于生成结构模板文件
import gzip
import struct


class Byte(int):
    pass


class Short(int):
    pass


class Long(int):
    pass


class Double(float):
    pass


def _tag_id(value):
    # 根据 Python 类型推断 NBT 标签类型
    if isinstance(value, Byte):
        return 1
    if isinstance(value, Short):
        return 2
    if isinstance(value, Long):
        return 4
    if isinstance(value, bool):
        return 1
    if isinstance(value, int):
        return 3
    if isinstance(value, Double):
        return 6
    if isinstance(value, float):
        return 5
    if isinstance(value, str):
        return 8
    if isinstance(value, list):
        return 9
    if isinstance(value, dict):
        return 10
    raise TypeError("Unsupported NBT value: %r" % (value,))


def _string(text):
    data = text.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def _payload(value):
    # 按标签类型写出负载
    tag = _tag_id(value)
    if tag == 1:
        return struct.pack(">b", int(value))
    if tag == 2:
        return struct.pack(">h", int(value))
    if tag == 3:
        return struct.pack(">i", int(value))
    if tag == 4:
        return struct.pack(">q", int(value))
    if tag == 5:
        return struct.pack(">f", value)
    if tag == 6:
        return struct.pack(">d", value)
    if tag == 8:
        return _string(value)
    if tag == 9:
        element = 0
        if value:
            element = _tag_id(value[0])
        body = b"".join(_payload(item) for item in value)
        return struct.pack(">bi", element, len(value)) + body
    parts = []
    for key, item in value.items():
        parts.append(struct.pack(">b", _tag_id(item)) + _string(key) + _payload(item))
    return b"".join(parts) + b"\x00"


def write_nbt(path, root):
    # 写出 gzip 压缩的根复合标签
    data = struct.pack(">b", 10) + _string("") + _payload(root)
    with gzip.open(path, "wb") as handle:
        handle.write(data)
