/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;

public final class Service {
  private final ProtoType protoType;
  private final ServiceElement element;
  private final ImmutableList<Rpc> rpcs;
  private final Options options;

  private Service(ProtoType protoType, ServiceElement element,
      ImmutableList<Rpc> rpcs, Options options) {
    this.protoType = protoType;
    this.element = element;
    this.rpcs = rpcs;
    this.options = options;
  }

  public static Service get(ProtoType protoType, ServiceElement element) {
    ImmutableList.Builder<Rpc> rpcs = ImmutableList.builder();
    for (RpcElement rpc : element.rpcs()) {
      rpcs.add(new Rpc(rpc, new Options(Options.METHOD_OPTIONS, rpc.options())));
    }

    Options options = new Options(Options.SERVICE_OPTIONS, element.options());

    return new Service(protoType, element, rpcs.build(), options);
  }

  public Location location() {
    return element.location();
  }

  public ProtoType type() {
    return protoType;
  }

  public String documentation() {
    return element.documentation();
  }

  public ImmutableList<Rpc> rpcs() {
    return rpcs;
  }

  /** Returns the RPC named {@code name}, or null if this service has no such method. */
  public Rpc rpc(String name) {
    for (Rpc rpc : rpcs) {
      if (rpc.name().equals(name)) {
        return rpc;
      }
    }
    return null;
  }

  public Options options() {
    return options;
  }

  void link(Linker linker) {
    linker = linker.withContext(this);
    for (Rpc rpc : rpcs) {
      rpc.link(linker);
    }
  }

  void linkOptions(Linker linker) {
    linker = linker.withContext(this);
    for (Rpc rpc : rpcs) {
      rpc.linkOptions(linker);
    }
    options.link(linker);
  }

  void validate(Linker linker) {
    linker = linker.withContext(this);
    for (Rpc rpc : rpcs) {
      rpc.validate(linker);
    }
  }

  Service retainAll(Schema schema, MarkSet markSet) {
    // If this service is not retained, prune it.
    if (!markSet.contains(protoType)) {
      return null;
    }

    ImmutableList.Builder<Rpc> retainedRpcs = ImmutableList.builder();
    for (Rpc rpc : rpcs) {
      Rpc retainedRpc = rpc.retainAll(schema, markSet);
      if (retainedRpc != null && markSet.contains(ProtoMember.get(protoType, rpc.name()))) {
        retainedRpcs.add(retainedRpc);
      }
    }

    return new Service(protoType, element, retainedRpcs.build(),
        options.retainAll(schema, markSet));
  }
}
